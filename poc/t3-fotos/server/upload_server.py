import os
from http.server import BaseHTTPRequestHandler, HTTPServer

UPLOADS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")
os.makedirs(UPLOADS_DIR, exist_ok=True)


class UploadHandler(BaseHTTPRequestHandler):
    def do_PUT(self):
        length = int(self.headers.get("Content-Length", 0))
        data = self.rfile.read(length)
        if length > 300_000:
            self.send_response(422)
            self.end_headers()
            return
        filename = os.path.basename(self.path) or "test.jpg"
        with open(os.path.join(UPLOADS_DIR, filename), "wb") as f:
            f.write(data)
        self.send_response(204)
        self.end_headers()


if __name__ == "__main__":
    print("Receptor T3 escuchando en 0.0.0.0:8082 (PUT /<archivo>)")
    HTTPServer(("0.0.0.0", 8082), UploadHandler).serve_forever()