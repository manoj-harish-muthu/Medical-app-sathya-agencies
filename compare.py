import re

with open('src/main/resources/fxml/Sidebar.fxml', 'r', encoding='utf-8') as f:
    fxml = f.read()

with open('src/main/java/com/pharmacyerp/controller/SidebarController.java', 'r', encoding='utf-8') as f:
    java = f.read()

fxml_handlers = set(re.findall(r'onAction="#([^"]+)"', fxml))
java_handlers = set(re.findall(r'void\s+([a-zA-Z0-9_]+)\(', java))

missing_in_java = fxml_handlers - java_handlers
print("Missing in Java:", missing_in_java)
