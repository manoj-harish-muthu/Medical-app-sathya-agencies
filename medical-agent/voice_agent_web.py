"""
Sathya Agencies — Doxy AI Voice Web Server (Option C).

Runs a lightweight, self-contained web server that serves a 1-page browser UI on http://localhost:8080
and bridges Chrome's native WebRTC microphone (with Acoustic Echo Cancellation & Noise Suppression)
directly to Gemini Multimodal Live API.

Run from terminal:
    python voice_agent_web.py
    python voice_agent_web.py --port 8080
"""

import os
import sys
import json
import asyncio
import argparse
import webbrowser
from pathlib import Path
from dotenv import load_dotenv

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

load_dotenv()

from google import genai
from google.genai import types

from websockets.asyncio.server import serve
from websockets.http11 import Response
from websockets.datastructures import Headers

# Constants
DEFAULT_PORT = 8080
LIVE_MODEL = "gemini-2.5-flash-native-audio-latest"
VOICE_NAME = "Aoede"

SYSTEM_PROMPT = """
You are Doxy, the friendly, helpful AI voice order-taking agent for Sathya Agencies pharmacy in Tamil Nadu.
You are speaking directly with a customer on a live voice call.

LANGUAGE & SPEECH RECOGNITION (ASR) PRIMING:
- Customers speak in English, Tamil, or Tanglish (spoken modern Tamil mixed with English words).
- Listen carefully and accurately transcribe customer speech.
- Recognize common Indian pharmaceutical medicine names and brands even when spoken fast or with regional accents:
  * Dolo 650 (often heard as "dolo", "dolo six fifty", "dolo 650")
  * Paracetamol (often heard as "paracetamol", "parasitamol", "paracip 650")
  * Met XL 25 / 50 (often heard as "met xl", "mete excel", "met-xl", "met xl 25")
  * Pan-DSR / Pan-D / Pantocid
  * Febura 20mg / Febuxostat
  * Azithral 500mg / Azithromycin
  * Cetirizine 10mg / Cetzine
  * Benadryl Cough Syrup / Ascoril / Chericof
  * Telma 40 / Telmisartan
  * Shelcal 500 / Calcirol
  * Montair-LC / Montek-LC
  * Amoxicillin / Augmentin 625
- When customers say numbers and units: e.g. "one strip", "two tablets", "10 tablets", "1 bottle (100ml)", understand them accurately.

VOICE CONVERSATION GUIDELINES:
1. Speak naturally, warmly, and concisely (1 to 2 short sentences per response).
2. If the customer speaks Tamil or Tanglish, reply in friendly, modern spoken Tamil / Tanglish (e.g. "கண்டிப்பா, Dolo 650 ஒரு strip எடுத்துக்கிறேன்...").
3. Do not rush to respond: wait patiently until the customer has completely finished speaking their order.
4. Help them order medicines and collect quantities (strips, tablets, bottles).
5. For syrups/tonics, confirm bottle volume in ml (e.g. 100ml).
6. Collect delivery details: customer name, phone number, and address with PIN code.
7. You are an order collector, not a doctor. Never give medical advice or diagnose diseases.
"""

HTML_FILE_PATH = Path(__file__).parent / "web" / "index.html"


def load_html_content() -> bytes:
    """Load the single-page HTML interface."""
    if HTML_FILE_PATH.exists():
        return HTML_FILE_PATH.read_bytes()
    return b"<h1>Error: web/index.html not found</h1>"


def process_http_request(connection, request):
    """Serve the Web UI over HTTP and pass through WebSocket upgrades."""
    path = request.path.split("?")[0]
    if path in ("/", "/index.html"):
        html_bytes = load_html_content()
        headers = Headers([
            ("Content-Type", "text/html; charset=utf-8"),
            ("Cache-Control", "no-cache"),
        ])
        return Response(200, "OK", headers, html_bytes)
    elif path == "/favicon.ico":
        return Response(204, "No Content", Headers([]), b"")
    # Return None so websockets library processes the WebSocket handshake on /ws or any other path
    return None


class BrowserVoiceSession:
    def __init__(self, ws, client: genai.Client):
        self.ws = ws
        self.client = client
        self.active = False
        self.live_session = None
        self.user_speaking = False
        self.doxy_speaking = False

    async def handle_connection(self):
        """Main WebSocket loop handling browser messages."""
        try:
            async for message in self.ws:
                if isinstance(message, str):
                    try:
                        data = json.loads(message)
                        msg_type = data.get("type")
                        if msg_type == "start_call":
                            await self.start_live_call()
                        elif msg_type == "end_call":
                            await self.stop_live_call()
                    except json.JSONDecodeError:
                        pass
                elif isinstance(message, (bytes, bytearray)):
                    # Binary 16kHz PCM audio chunk from Chrome microphone
                    if self.active and self.live_session:
                        try:
                            await self.live_session.send_realtime_input(
                                media=types.Blob(
                                    data=bytes(message),
                                    mime_type="audio/pcm;rate=16000"
                                )
                            )
                        except Exception as e:
                            print(f"[Send to Gemini Error] {e}", flush=True)

        except Exception as e:
            print(f"[WebSocket Connection Closed] {e}", flush=True)
        finally:
            await self.stop_live_call()

    async def start_live_call(self):
        """Initiate Gemini Live multimodal session and pipe responses to browser."""
        if self.active:
            return

        print("\n📞 [Call Started] Browser requested live call with Doxy.", flush=True)
        self.active = True
        self.user_speaking = False
        self.doxy_speaking = False

        config = types.LiveConnectConfig(
            response_modalities=["AUDIO"],
            realtime_input_config=types.RealtimeInputConfig(
                automatic_activity_detection=types.AutomaticActivityDetection(
                    start_of_speech_sensitivity=types.StartSensitivity.START_SENSITIVITY_HIGH,
                    end_of_speech_sensitivity=types.EndSensitivity.END_SENSITIVITY_LOW,
                    prefix_padding_ms=100,
                    silence_duration_ms=800,  # Snappy human phone call pacing: 800ms silence after user finishes speaking
                )
            ),
            input_audio_transcription=types.AudioTranscriptionConfig(),
            output_audio_transcription=types.AudioTranscriptionConfig(),
            system_instruction=types.Content(
                parts=[types.Part.from_text(text=SYSTEM_PROMPT)]
            ),
            speech_config=types.SpeechConfig(
                voice_config=types.VoiceConfig(
                    prebuilt_voice_config=types.PrebuiltVoiceConfig(voice_name=VOICE_NAME)
                )
            ),
        )

        async def run_gemini():
            try:
                async with self.client.aio.live.connect(model=LIVE_MODEL, config=config) as session:
                    self.live_session = session
                    print("✅ [Gemini Live Connected] Ready for bi-directional audio streaming.", flush=True)

                    # Trigger warm greeting
                    await session.send_client_content(
                        turns=[
                            types.Content(
                                role="user",
                                parts=[types.Part.from_text(
                                    text="[Customer dialed Sathya Agencies hotline from browser. Greet them warmly and ask how you can help with their medicine order.]"
                                )]
                            )
                        ],
                        turn_complete=True
                    )

                    # Continuous receive loop across all conversation turns
                    while self.active:
                        try:
                            async for response in session.receive():
                                if not self.active:
                                    break

                                server_content = response.server_content
                                if server_content is not None:
                                    # 1. User interruption / barge-in
                                    if server_content.interrupted:
                                        self.doxy_speaking = False
                                        await self.ws.send(json.dumps({"type": "interrupted"}))
                                        continue

                                    # 2. Real-time User Speech Transcription (Preserve spaces between syllable tokens)
                                    if server_content.input_transcription and getattr(server_content.input_transcription, "text", None):
                                        u_text = server_content.input_transcription.text
                                        if u_text:
                                            if not self.user_speaking:
                                                print("\n🗣️ You: ", end="", flush=True)
                                                self.user_speaking = True
                                            print(u_text, end="", flush=True)
                                            await self.ws.send(json.dumps({
                                                 "type": "user_transcription",
                                                 "text": u_text
                                             }))

                                    # 3. Spoken Audio Chunks (24kHz PCM)
                                    if server_content.model_turn is not None:
                                        self.user_speaking = False
                                        for part in server_content.model_turn.parts:
                                            if part.inline_data and part.inline_data.data:
                                                # Send raw binary audio frame directly to browser Web Audio API
                                                await self.ws.send(part.inline_data.data)

                                    # 4. Real-time Doxy Speech Transcription
                                    if server_content.output_transcription and getattr(server_content.output_transcription, "text", None):
                                        d_text = server_content.output_transcription.text
                                        if not self.doxy_speaking:
                                            print("\n💬 Doxy: ", end="", flush=True)
                                            self.doxy_speaking = True
                                        print(d_text, end="", flush=True)
                                        await self.ws.send(json.dumps({
                                            "type": "doxy_transcription",
                                            "text": d_text
                                        }))

                                    # 5. Turn Complete
                                    if server_content.turn_complete:
                                        self.user_speaking = False
                                        self.doxy_speaking = False
                                        await self.ws.send(json.dumps({"type": "turn_complete"}))
                        except Exception as turn_e:
                            if not self.active:
                                break
                            print(f"[Turn Loop Event] {turn_e}", flush=True)
                            await asyncio.sleep(0.05)

            except asyncio.CancelledError:
                pass
            except Exception as e:
                print(f"[Gemini Live Session Error] {e}", flush=True)
                try:
                    await self.ws.send(json.dumps({
                        "type": "error",
                        "message": str(e)
                    }))
                except Exception:
                    pass
            finally:
                self.live_session = None

        self.gemini_task = asyncio.create_task(run_gemini())

    async def stop_live_call(self):
        """Cleanly terminate the call session."""
        self.active = False
        if hasattr(self, "gemini_task") and self.gemini_task:
            self.gemini_task.cancel()
            self.gemini_task = None
        self.live_session = None
        print("🛑 [Call Ended] Session terminated.", flush=True)


async def main():
    parser = argparse.ArgumentParser(description="Sathya Agencies Doxy Voice Web UI Server")
    parser.add_argument("--port", "-p", type=int, default=DEFAULT_PORT, help="Port to serve the Web UI on")
    parser.add_argument("--no-browser", action="store_true", help="Do not automatically open the browser")
    args = parser.parse_args()

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        print("❌ ERROR: GEMINI_API_KEY is not set in your .env file.")
        sys.exit(1)

    client = genai.Client(api_key=api_key)
    # Prevent 1011 keepalive ping timeout with Google AI Studio WebSockets
    client._api_client._websocket_ssl_ctx['ping_interval'] = None
    client._api_client._websocket_ssl_ctx['ping_timeout'] = None

    async def ws_handler(websocket):
        session = BrowserVoiceSession(websocket, client)
        await session.handle_connection()

    url = f"http://localhost:{args.port}/"
    print("=" * 68)
    print("🌐 SATHYA AGENCIES — DOXY BROWSER VOICE UI (OPTION C)")
    print("=" * 68)
    print(f"⚡ Model:        {LIVE_MODEL}")
    print(f"🗣️ Voice:        {VOICE_NAME} (Low Latency Native Audio)")
    print(f"🛡️ Processing:   Chrome WebRTC AEC3 (Acoustic Echo Cancellation)")
    print(f"🔗 Local Web UI: {url}")
    print("=" * 68)
    print(f"🚀 Server started! Open {url} in Chrome to talk to Doxy.")
    print("🛑 Press Ctrl+C in this terminal to stop the server.\n")

    if not args.no_browser:
        # Open in Chrome / default browser after a brief moment
        async def open_browser_later():
            await asyncio.sleep(1.0)
            webbrowser.open(url)
        asyncio.create_task(open_browser_later())

    async with serve(ws_handler, "0.0.0.0", args.port, process_request=process_http_request):
        await asyncio.Future()  # Run forever


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n\n👋 Voice Web server stopped cleanly.")
