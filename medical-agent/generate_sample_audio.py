import os
import subprocess

vbs_code = """
Set voice = CreateObject("SAPI.SpVoice")
Set fs = CreateObject("SAPI.SpFileStream")
fs.Open "sample_voice_order.wav", 3
Set voice.AudioOutputStream = fs
voice.Speak "Hello, I need two strips of Dolo 650 and one strip of Cetirizine. My name is Arun and my delivery address is Anna Nagar."
fs.Close
"""

vbs_path = "temp_gen.vbs"
with open(vbs_path, "w", encoding="utf-8") as f:
    f.write(vbs_code)

try:
    subprocess.run(["cscript", "//nologo", vbs_path], check=True)
    if os.path.exists("sample_voice_order.wav"):
        print(f"Sample audio generated successfully: sample_voice_order.wav ({os.path.getsize('sample_voice_order.wav')} bytes)")
finally:
    if os.path.exists(vbs_path):
        os.remove(vbs_path)
