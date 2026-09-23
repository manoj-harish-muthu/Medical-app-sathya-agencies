import os
import glob

controller_dir = r"c:\project\Medical-app-sathya-agencies\src\main\java\com\pharmacyerp\controller"

for filepath in glob.glob(os.path.join(controller_dir, "*.java")):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # We want to remove blocks like:
    # if (!success) {
    #     records.add(new MasterData(1, "Sample...", ...));
    #     records.add(new MasterData(2, "Sample...", ...));
    # }
    import re
    # Match the block exactly
    new_content = re.sub(r'if\s*\(!success\)\s*\{\s*records\.add\(new MasterData\(1,\s*"Sample.*?\);\s*records\.add\(new MasterData\(2,\s*"Sample.*?\);\s*\}', '', content, flags=re.DOTALL)
    
    # Let's also handle the ones like HistoryData (e.g. MedDatabaseController)
    new_content = re.sub(r'private void loadDummyData\(\) \{.*?\}', 'private void loadDummyData() {\n        // Dummy data removed as per user request\n    }', new_content, flags=re.DOTALL)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Removed dummy data from {os.path.basename(filepath)}")

print("Done removing dummy data.")
