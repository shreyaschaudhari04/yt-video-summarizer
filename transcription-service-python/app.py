from flask import Flask, request, jsonify
from youtube_transcript_api import YouTubeTranscriptApi
import re

app = Flask(__name__)

# Extract YouTube Video ID
def extract_video_id(url):
    match = re.search(r"(?:v=|youtu\.be/)([a-zA-Z0-9_-]{11})", url)
    return match.group(1) if match else None


# Get available transcript languages
@app.route('/languages', methods=['POST'])
def get_languages():
    try:
        data = request.get_json()

        if not data or "url" not in data:
            return jsonify({"error": "Missing URL in request body"}), 400

        url = data.get("url")
        video_id = extract_video_id(url)

        if not video_id:
            return jsonify({"error": "Invalid YouTube URL"}), 400

        ytt_api = YouTubeTranscriptApi()
        transcript_list = ytt_api.list(video_id)

        available = list(transcript_list)

        if not available:
            return jsonify({"error": "No transcripts available"}), 400

        languages = []
        for t in available:
            languages.append({
                "language": t.language,
                "language_code": t.language_code,
                "is_generated": t.is_generated
            })

        return jsonify({
            "video_id": video_id,
            "languages": languages
        })

    except Exception as e:
        print("ERROR /languages:", str(e))
        return jsonify({"error": str(e)}), 500



# Get transcript
@app.route('/transcript', methods=['POST'])
def get_transcript():
    try:
        data = request.get_json()

        if not data or "url" not in data:
            return jsonify({"error": "Missing URL in request body"}), 400

        url = data.get("url")
        requested_language_code = data.get("language_code")  # optional
        video_id = extract_video_id(url)

        if not video_id:
            return jsonify({"error": "Invalid YouTube URL"}), 400

        ytt_api = YouTubeTranscriptApi()
        transcript_list = ytt_api.list(video_id)
        available = list(transcript_list)

        if not available:
            return jsonify({"error": "No transcripts available"}), 400

        selected_transcript = None
        transcript_data = None

        if requested_language_code:
            # Exact match
            for t in available:
                if t.language_code == requested_language_code:
                    selected_transcript = t
                    transcript_data = t.fetch()
                    break

            if not transcript_data:
                for t in available:
                    if t.language_code.startswith(requested_language_code):
                        selected_transcript = t
                        transcript_data = t.fetch()
                        break

        # 2. If not found, prefer manual transcript
        if not transcript_data:
            for t in available:
                if not t.is_generated:
                    selected_transcript = t
                    transcript_data = t.fetch()
                    break

        # 3. Fallback to first available transcript
        if not transcript_data:
            selected_transcript = available[0]
            transcript_data = selected_transcript.fetch()

        # Build transcript text
        text = " ".join([t.text for t in transcript_data])

        # Also return all available languages
        languages = []
        for t in available:
            languages.append({
                "language": t.language,
                "language_code": t.language_code,
                "is_generated": t.is_generated
            })

        return jsonify({
            "video_id": video_id,
            "transcript": text,

            "language": selected_transcript.language,
            "language_code": selected_transcript.language_code,

            "selected_language": {
                "language": selected_transcript.language,
                "language_code": selected_transcript.language_code,
                "is_generated": selected_transcript.is_generated
            },
            "available_languages": languages
        })

    except Exception as e:
        print("ERROR /transcript:", str(e))
        return jsonify({"error": str(e)}), 500


# Health check (optional but useful)
@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(port=5000, debug=True)