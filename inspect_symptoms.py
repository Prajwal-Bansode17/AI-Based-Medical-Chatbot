import ast
from pathlib import Path

p = Path("ml/api.py")
src = p.read_text(encoding="utf-8")
tree = ast.parse(src)

wanted = {"normalize_text", "extract_symptoms", "detect_language"}

for node in tree.body:
    if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.name in wanted:
        print(f"\n{'='*20} {node.name} {'='*20}")
        print(ast.get_source_segment(src, node))

    elif isinstance(node, ast.Assign):
        names = [t.id for t in node.targets if isinstance(t, ast.Name)]
        if "SYMPTOM_ALIASES" in names:
            print(f"\n{'='*20} SYMPTOM_ALIASES {'='*20}")
            print(ast.get_source_segment(src, node))
