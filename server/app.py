#!/usr/bin/env python
import os
import logging
from flask import Flask, jsonify, send_from_directory
from flask_cors import CORS

from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__, static_folder="static")

app.app_context()

@app.route("/", defaults={"path": "index.html"})
def static_file(path: str):
    if "." not in path:
        path += ".html"
        
    return send_from_directory(app.static_folder, path)

@app.errorhandler(404)
def not_found(e):
    return jsonify({ "status": "not found" }), 404

@app.route("/api/health")
def health_check():
    return jsonify({ "status": "ok" }), 200

if __name__ == "__main__":
    environment = os.environ.get("ENVIRONMENT", "development")
    
    if environment == "production":
        app.run(port=8080)
    else:
        CORS(app, resources={r"/api/*": { "origins": "*" }})
        logging.basicConfig(level = logging.DEBUG)
        app.run(port=8080, debug=True)
        