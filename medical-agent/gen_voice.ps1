
Add-Type -AssemblyName System.Speech
 = New-Object System.Speech.Synthesis.SpeechSynthesizer
.SetOutputToWaveFile('sample_voice_order.wav')
.Speak('Hello, I need two strips of Dolo 650 and one strip of Cetirizine. My name is Arun and my delivery address is Anna Nagar.')
.Dispose()
