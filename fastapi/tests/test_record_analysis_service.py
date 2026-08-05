import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import HTTPException, status

from app.services.record_analysis_service import analyze_record


class RecordAnalysisServiceTest(unittest.TestCase):
    @patch("app.services.record_analysis_service.get_settings")
    def test_analyze_record_rejects_request_when_gemini_key_is_absent(self, get_settings):
        get_settings.return_value = SimpleNamespace(gemini_api_key=None)

        with self.assertRaises(HTTPException) as context:
            analyze_record(None)

        self.assertEqual(context.exception.status_code, status.HTTP_503_SERVICE_UNAVAILABLE)


if __name__ == "__main__":
    unittest.main()
