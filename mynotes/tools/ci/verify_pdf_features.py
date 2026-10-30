"""Independent pypdf verification of bounded, synthetic Android export fixtures only."""
import argparse
import json
from pathlib import Path
import tarfile
import tempfile
from urllib.parse import quote


def verify(archive):
    from pypdf import PdfReader
    expected = {"canonical-links.pdf", "legacy-link.pdf", "guides-off.pdf", "guides-on.pdf"}
    reports = {}
    with tempfile.TemporaryDirectory(prefix="notes-pdf-review-") as folder:
        with tarfile.open(archive) as source:
            total = 0
            for member in source.getmembers():
                path = Path(member.name)
                if member.isdir():
                    continue
                if not member.isfile() or path.is_absolute() or ".." in path.parts or len(path.parts) != 2 or path.parts[0] != "pdf-review" or path.name not in expected:
                    raise ValueError("Unexpected synthetic PDF archive member")
                total += member.size
                if member.size > 32 * 1024 * 1024 or total > 128 * 1024 * 1024:
                    raise ValueError("PDF fixture size bound")
                destination = Path(folder) / path.name
                if destination.exists():
                    raise ValueError("Duplicate PDF fixture")
                with source.extractfile(member) as input_file, destination.open("wb") as output:
                    while chunk := input_file.read(8192):
                        output.write(chunk)
        if {path.name for path in Path(folder).iterdir()} != expected:
            raise ValueError("Missing PDF fixture; do not waive native export tests")
        for name in sorted(expected):
            reader = PdfReader(Path(folder) / name, strict=True)
            assert not reader.is_encrypted
            links = []
            for page_number, page in enumerate(reader.pages, 1):
                assert tuple(float(v) for v in page.mediabox) == (0, 0, 595, 842)
                for reference in page.get("/Annots", []):
                    annotation = reference.get_object()
                    assert annotation["/Subtype"] == "/Link"
                    action = annotation["/A"].get_object()
                    assert action["/S"] == "/URI" and "/JS" not in action
                    uri = str(action["/URI"])
                    assert uri.startswith("https://example.com/")
                    left, bottom, right, top = map(float, annotation["/Rect"])
                    assert 48 <= left < right <= 547 and 84 <= bottom < top <= 794
                    links.append({"page": page_number, "uri": uri, "rect": [left, bottom, right, top]})
            if name == "canonical-links.pdf":
                assert len(reader.pages) > 2 and len(links) > 10
                body = [link for link in links if link["uri"] == "https://example.com/notes?q=one&lang=kn"]
                table = [link for link in links if link["uri"] == "https://example.com/table"]
                assert len({link["page"] for link in body}) > 1 and table
                assert min(link["page"] for link in table) >= max(link["page"] for link in body)
                assert len(body) + len(table) == len(links)
            elif name == "legacy-link.pdf":
                assert len(reader.pages) == 1 and len(links) == 1
                assert links[0]["uri"] == "https://example.com/" + quote("ಪದ") + "?q=one(two)"
            else:
                assert len(reader.pages) == 1 and not links
            reports[name] = {"pages": len(reader.pages), "link_rectangles": len(links), "links": links}
    return {"ok": True, "parser": "pypdf", "fixtures": reports}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    result = verify(args.archive)
    Path(args.output).write_text(json.dumps(result, indent=2) + "\n")
    print("Independent PDF annotations/page geometry: PASS")


if __name__ == "__main__":
    main()
