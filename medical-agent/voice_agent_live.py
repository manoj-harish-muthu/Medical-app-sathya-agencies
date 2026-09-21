"""
Real-time Voice Agent (Doxy) for Sathya Agencies using Gemini Multimodal Live API.

Features:
- Native Full-Duplex Real-Time Voice Conversation (like Gemini Live in Gemini App).
- Real-time live input audio streaming via WebSockets with media=Blob(...).
- Fix for 1011 keepalive ping timeout (ping_interval disabled).
- Native hardware rate capture (48kHz WASAPI / 44.1kHz MME) with zero-latency 16kHz resampling.
- Universal speaker output support (24kHz direct MME / upsampled WASAPI).
- Live user speech transcription displayed on-screen as you speak.
- Live Doxy spoken audio streaming (24kHz PCM) + real-time speech transcription.
- Natural barge-in / interruption: as soon as you speak, Doxy instantly pauses and listens.
- Speaker echo bleed protection for laptop microphone/speakers.
- Zero-latency thinking mode (thinking_budget=0) for conversational phone speed.
- Native multi-language support (Tamil, Tanglish, English).

Run from terminal:
    python voice_agent_live.py
    python voice_agent_live.py --list-devices
    python voice_agent_live.py --device <index>
"""

import os
import sys
import time
import queue
import asyncio
import argparse
import threading
import numpy as np
import sounddevice as sd
from dotenv import load_dotenv

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

load_dotenv()

from google import genai
from google.genai import types

# Audio Stream Settings
GEMINI_INPUT_RATE = 16000   # 16kHz PCM for Gemini Live input
GEMINI_OUTPUT_RATE = 24000  # 24kHz PCM from Gemini Live output
TARGET_CHUNK_MS = 64        # ~64ms per chunk

# Live Model & Voice
LIVE_MODEL = "gemini-2.5-flash-native-audio-latest"
VOICE_NAME = "Aoede"  # Options: Aoede, Puck, Charon, Kore, Fenrir

SYSTEM_PROMPT = """
You are Doxy, the friendly, helpful AI voice order-taking agent for Sathya Agencies pharmacy in Tamil Nadu.
You are speaking directly with a customer on a live phone call.

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


def select_best_audio_devices(preferred_in=None, preferred_out=None):
    """Select the best input and output devices."""
    devices = sd.query_devices()

    # Select input device: prefer WASAPI for low latency and native 48kHz
    in_idx = preferred_in
    if in_idx is None:
        for i, d in enumerate(devices):
            if d['max_input_channels'] > 0:
                host = sd.query_hostapis(d['hostapi'])['name']
                if 'WASAPI' in host and 'Microphone' in d['name']:
                    in_idx = i
                    break
        if in_idx is None:
            in_idx = sd.default.device[0]

    # Select output device: default output is usually MME/DirectSound which supports 24kHz directly
    out_idx = preferred_out
    if out_idx is None:
        out_idx = sd.default.device[1]

    in_info = sd.query_devices(in_idx)
    out_info = sd.query_devices(out_idx)

    # Determine output stream capabilities
    try:
        sd.check_output_settings(device=out_idx, samplerate=GEMINI_OUTPUT_RATE, channels=1, dtype="int16")
        out_rate = GEMINI_OUTPUT_RATE
        out_channels = 1
        out_convert = False
    except Exception:
        out_rate = int(out_info['default_samplerate'])
        out_channels = min(2, max(1, out_info['max_output_channels']))
        out_convert = True

    return in_idx, in_info, out_idx, out_info, out_rate, out_channels, out_convert


class LiveVoiceAgent:
    def __init__(self, in_device_idx=None, out_device_idx=None, echo_gate=True):
        api_key = os.getenv("GEMINI_API_KEY")
        if not api_key:
            raise ValueError("GEMINI_API_KEY is missing in your .env file.")

        self.client = genai.Client(api_key=api_key)
        # CRITICAL FIX: Disable client-side ping frames to prevent 1011 keepalive ping timeout
        self.client._api_client._websocket_ssl_ctx['ping_interval'] = None
        self.client._api_client._websocket_ssl_ctx['ping_timeout'] = None

        self.echo_gate = echo_gate
        self.audio_out_queue = queue.Queue()
        self.audio_in_queue = asyncio.Queue()
        self.stop_event = threading.Event()
        self.loop = None
        self.doxy_speaking = False
        self.user_speaking = False
        self.last_meter_time = 0

        # Audio device setup
        (
            self.in_idx,
            self.in_info,
            self.out_idx,
            self.out_info,
            self.out_rate,
            self.out_channels,
            self.out_convert,
        ) = select_best_audio_devices(preferred_in=in_device_idx, preferred_out=out_device_idx)

        self.in_rate = int(self.in_info['default_samplerate'])
        self.in_channels = min(2, max(1, self.in_info['max_input_channels']))
        self.in_blocksize = int(self.in_rate * (TARGET_CHUNK_MS / 1000.0))

    def audio_playback_worker(self):
        """Dedicated thread writing audio chunks to the speakers."""
        try:
            with sd.RawOutputStream(
                samplerate=self.out_rate,
                channels=self.out_channels,
                dtype="int16",
                blocksize=1024,
                device=self.out_idx,
            ) as out_stream:
                while not self.stop_event.is_set():
                    try:
                        raw_data = self.audio_out_queue.get(timeout=0.05)
                        if self.out_convert:
                            samples = np.frombuffer(raw_data, dtype=np.int16)
                            if self.out_rate == 48000:
                                resampled = np.repeat(samples, 2)
                            else:
                                orig_len = len(samples)
                                target_len = int(orig_len * self.out_rate / GEMINI_OUTPUT_RATE)
                                orig_idx = np.linspace(0, orig_len - 1, target_len)
                                resampled = np.interp(orig_idx, np.arange(orig_len), samples).astype(np.int16)
                            if self.out_channels == 2:
                                stereo = np.column_stack((resampled, resampled))
                                out_stream.write(stereo.tobytes())
                            else:
                                out_stream.write(resampled.tobytes())
                        else:
                            out_stream.write(raw_data)
                    except queue.Empty:
                        continue
                    except Exception as e:
                        if not self.stop_event.is_set():
                            print(f"\n[Playback Warning] {e}", flush=True)
        except Exception as e:
            print(f"\n[Speaker Error] Could not open output device {self.out_idx}: {e}", flush=True)

    def mic_callback(self, indata, frames, time_info, status):
        """Sounddevice callback capturing microphone input at native hardware rate."""
        if self.stop_event.is_set():
            return

        # 1. Convert stereo to mono if needed
        if indata.shape[1] > 1:
            mono = ((indata[:, 0].astype(np.int32) + indata[:, 1].astype(np.int32)) // 2).astype(np.int16)
        else:
            mono = indata[:, 0]

        # 2. Resample to 16000 Hz for Gemini Live
        if self.in_rate == 48000:
            resampled = mono[::3]  # Exact 3:1 integer decimation
        elif self.in_rate == 16000:
            resampled = mono
        else:
            orig_len = len(mono)
            target_len = int(orig_len * GEMINI_INPUT_RATE / self.in_rate)
            orig_idx = np.linspace(0, orig_len - 1, target_len)
            resampled = np.interp(orig_idx, np.arange(orig_len), mono).astype(np.int16)

        # 3. Peak volume & Activity Detection
        peak = int(np.max(np.abs(resampled))) if len(resampled) > 0 else 0

        # Visual level meter (when user speaks)
        now = time.time()
        if peak > 1000 and not self.doxy_speaking and (now - self.last_meter_time > 0.25):
            self.last_meter_time = now
            bars = min(10, int(peak / 2000) + 1)
            meter = "█" * bars + "░" * (10 - bars)
            print(f"\r🎤 [Speaking {meter}]", end="", flush=True)

        # 4. Echo Gate: Prevent laptop speaker bleed from interrupting Doxy
        if self.echo_gate and self.doxy_speaking:
            # During Doxy's speech, only forward if peak exceeds speaker bleed threshold
            if peak < 3500:
                return

        # 5. Push 16kHz PCM bytes to async queue
        if self.loop and not self.stop_event.is_set():
            self.loop.call_soon_threadsafe(self.audio_in_queue.put_nowait, resampled.tobytes())

    def flush_audio_output(self):
        """Immediately clear playback buffer when user interrupts (barge-in)."""
        while not self.audio_out_queue.empty():
            try:
                self.audio_out_queue.get_nowait()
            except queue.Empty:
                break

    async def send_mic_audio(self, session):
        """Continuously stream microphone audio chunks to Gemini Live."""
        try:
            while not self.stop_event.is_set():
                pcm_data = await self.audio_in_queue.get()
                # Must use media=Blob(...) so MLDev WebSocket receives mediaChunks
                await session.send_realtime_input(
                    media=types.Blob(
                        data=pcm_data,
                        mime_type=f"audio/pcm;rate={GEMINI_INPUT_RATE}"
                    )
                )
        except asyncio.CancelledError:
            pass
        except Exception as e:
            if not self.stop_event.is_set():
                print(f"\n[Mic Streaming Error] {e}", flush=True)

    async def receive_gemini_responses(self, session):
        """Receive live audio and transcription events from Gemini Live across all turns."""
        try:
            while not self.stop_event.is_set():
                try:
                    async for response in session.receive():
                        if self.stop_event.is_set():
                            break

                        server_content = response.server_content
                        if server_content is not None:
                            # User interrupted Doxy (barge-in)
                            if server_content.interrupted:
                                self.flush_audio_output()
                                print("\n🛑 [Interrupted - Listening to you...]", flush=True)
                                self.doxy_speaking = False
                                continue

                            # Live User Speech Transcription (Accumulate tokens into a clean sentence)
                            if server_content.input_transcription and getattr(server_content.input_transcription, "text", None):
                                u_text = server_content.input_transcription.text
                                if u_text:
                                    if not self.user_speaking:
                                        print("\n🗣️ You: ", end="", flush=True)
                                        self.user_speaking = True
                                    print(u_text, end="", flush=True)

                            # Model Spoken Audio Chunks
                            if server_content.model_turn is not None:
                                self.user_speaking = False
                                for part in server_content.model_turn.parts:
                                    if part.inline_data and part.inline_data.data:
                                        if not self.doxy_speaking:
                                            print("\n💬 Doxy: ", end="", flush=True)
                                            self.doxy_speaking = True
                                        self.audio_out_queue.put(part.inline_data.data)

                            # Live Doxy Output Speech Transcription
                            if server_content.output_transcription and getattr(server_content.output_transcription, "text", None):
                                doxy_text = server_content.output_transcription.text
                                if not self.doxy_speaking:
                                    print("\n💬 Doxy: ", end="", flush=True)
                                    self.doxy_speaking = True
                                print(doxy_text, end="", flush=True)

                            # Turn complete
                            if server_content.turn_complete:
                                self.user_speaking = False
                                self.doxy_speaking = False
                                print("\n🎙️ [Listening...] Speak anytime...", flush=True)
                except Exception as turn_err:
                    if self.stop_event.is_set():
                        break
                    print(f"\n[Turn Loop Notice] {turn_err}", flush=True)
                    await asyncio.sleep(0.05)

        except asyncio.CancelledError:
            pass
        except Exception as e:
            if not self.stop_event.is_set():
                print(f"\n[Receive Error] {e}", flush=True)

    async def run(self):
        self.loop = asyncio.get_running_loop()

        # Start background playback thread
        playback_thread = threading.Thread(target=self.audio_playback_worker, daemon=True)
        playback_thread.start()

        # Gemini Live Multimodal Configuration
        config = types.LiveConnectConfig(
            response_modalities=["AUDIO"],
            realtime_input_config=types.RealtimeInputConfig(
                automatic_activity_detection=types.AutomaticActivityDetection(
                    start_of_speech_sensitivity=types.StartSensitivity.START_SENSITIVITY_HIGH,
                    end_of_speech_sensitivity=types.EndSensitivity.END_SENSITIVITY_LOW,
                    prefix_padding_ms=100,
                    silence_duration_ms=600,  # Snappy human phone call pacing: 800ms silence after user finishes speaking
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

        in_host = sd.query_hostapis(self.in_info['hostapi'])['name']
        out_host = sd.query_hostapis(self.out_info['hostapi'])['name']

        print("=" * 65)
        print("📞 SATHYA AGENCIES - DOXY GEMINI LIVE VOICE AGENT")
        print("=" * 65)
        print(f"⚡ Model:     {LIVE_MODEL}")
        print(f"🎙️ Mic:       [{self.in_idx}] {self.in_info['name']} ({in_host}, {self.in_rate}Hz -> 16kHz)")
        print(f"🔊 Speaker:   [{self.out_idx}] {self.out_info['name']} ({out_host}, {self.out_rate}Hz)")
        print(f"🗣️ Voice:     {VOICE_NAME} (Low Latency Native Audio)")
        print(f"🛡️ Echo Gate: {'Enabled (laptop speaker protection)' if self.echo_gate else 'Disabled (headset mode)'}")
        print("🔗 Connecting to Gemini Multimodal Live API...")

        try:
            async with self.client.aio.live.connect(model=LIVE_MODEL, config=config) as session:
                print("✅ Connected to Gemini Live!")
                print("=" * 65)
                print("✨ Speak naturally into your microphone!")
                print("🛑 Press Ctrl+C at any time to end the call.")
                print("=" * 65)

                # Open microphone stream at native hardware rate
                with sd.InputStream(
                    device=self.in_idx,
                    samplerate=self.in_rate,
                    channels=self.in_channels,
                    dtype="int16",
                    blocksize=self.in_blocksize,
                    callback=self.mic_callback,
                ):
                    # Trigger initial greeting from Doxy
                    await session.send_client_content(
                        turns=[
                            types.Content(
                                role="user",
                                parts=[types.Part.from_text(
                                    text="[Customer dialed Sathya Agencies hotline. Greet them warmly and ask how you can help with their medicine order.]"
                                )]
                            )
                        ],
                        turn_complete=True
                    )

                    send_task = asyncio.create_task(self.send_mic_audio(session))
                    receive_task = asyncio.create_task(self.receive_gemini_responses(session))

                    await asyncio.gather(send_task, receive_task)

        except (asyncio.CancelledError, KeyboardInterrupt):
            pass
        finally:
            self.stop_event.set()
            self.flush_audio_output()
            print("\n\n📞 Call ended. Thank you for using Sathya Agencies Voice Agent!")


def print_devices():
    """Print available audio input and output devices."""
    print("\n=== AVAILABLE AUDIO INPUT DEVICES (MICROPHONES) ===")
    for i, d in enumerate(sd.query_devices()):
        if d['max_input_channels'] > 0:
            host = sd.query_hostapis(d['hostapi'])['name']
            rate = int(d['default_samplerate'])
            print(f"  [{i:2d}] {d['name']} ({host}, {rate}Hz, {d['max_input_channels']}ch)")

    print("\n=== AVAILABLE AUDIO OUTPUT DEVICES (SPEAKERS) ===")
    for i, d in enumerate(sd.query_devices()):
        if d['max_output_channels'] > 0:
            host = sd.query_hostapis(d['hostapi'])['name']
            rate = int(d['default_samplerate'])
            print(f"  [{i:2d}] {d['name']} ({host}, {rate}Hz, {d['max_output_channels']}ch)")
    print()


def main():
    parser = argparse.ArgumentParser(description="Sathya Agencies Gemini Live Voice Agent")
    parser.add_argument("--list-devices", action="store_true", help="List all available audio input and output devices")
    parser.add_argument("--device", "-d", type=int, default=None, help="Input microphone device index")
    parser.add_argument("--out-device", "-o", type=int, default=None, help="Output speaker device index")
    parser.add_argument("--no-echo-gate", action="store_true", help="Disable echo gate (recommended if wearing headphones)")
    args = parser.parse_args()

    if args.list_devices:
        print_devices()
        return

    agent = LiveVoiceAgent(
        in_device_idx=args.device,
        out_device_idx=args.out_device,
        echo_gate=not args.no_echo_gate,
    )
    try:
        asyncio.run(agent.run())
    except KeyboardInterrupt:
        print("\n\n👋 Voice agent exited cleanly.")


if __name__ == "__main__":
    main()
