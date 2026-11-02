# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Nocturne, please report it responsibly:

1. **Do not** open a public GitHub issue
2. Email the maintainers with details of the vulnerability
3. Include steps to reproduce, impact assessment, and any proof-of-concept
4. Allow reasonable time for a fix before public disclosure

We will acknowledge receipt within 48 hours and provide a timeline for remediation.

## Security Features

### Android
- **SAF (Storage Access Framework)**: Folder access uses system pickers with user-granted permissions
- **FileProvider**: Restricted to app-private directories only (`files-path`, `cache-path`). No external storage exposure
- **Minimal Permissions**: Only requests necessary permissions
- **No Network Access**: App operates entirely offline
- **Secure Storage**: All user data (playlists, history, settings) stored in app-private directories

### Desktop (JVM)
- **Local-Only**: No network communication
- **File System Access**: Only accesses user-selected music folders
- **No Telemetry**: No analytics, tracking, or data collection
- **Sandboxed**: Respects OS file permissions

## Best Practices for Users

- Only grant folder access to directories containing your music
- Keep your JDK/Android platform updated
- Download releases only from official sources
- Review app permissions on Android

## Known Considerations

- Desktop app requires Java 21+ runtime
- Android app targets modern Android versions
- All processing happens locally on your device
