# Pii-java / MindForge

## Guardian module environment variables

Before running the Guardian features, set the variables listed in `GUARDIAN_ENV.example`.

### Required keys
- `OPENAI_API_KEY`
- `GOOGLE_OAUTH_TOKEN`
- `MICROSOFT_GRAPH_TOKEN`
- `FIREBASE_SERVER_KEY`
- `SENDGRID_API_KEY`
- `DROPBOX_OAUTH_TOKEN`
- `AGORA_APP_ID`
- `AGORA_APP_CERT`
- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `DAILYCO_API_KEY`
- `GUARDIAN_JWT_SECRET`

## Notes
- `GOOGLE_OAUTH_TOKEN` is reused by both Google Drive and Google Calendar clients.
- `GUARDIAN_JWT_SECRET` has a development fallback in code, but you should set your own secret for real use.
- The Guardian video clients now read `TWILIO_AUTH_TOKEN` and `DAILYCO_API_KEY` from the environment instead of using hardcoded values.
