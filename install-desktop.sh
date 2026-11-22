#!/bin/bash
set -e
INSTALL_DIR=/opt/nocturne
ICON_DIR=/usr/share/pixmaps
DESKTOP_DIR=/usr/share/applications

echo "Installing Nocturne..."
sudo mkdir -p "$INSTALL_DIR"
sudo cp -r dist/* "$INSTALL_DIR/" 2>/dev/null || sudo cp -r nocturne/* "$INSTALL_DIR/"
sudo cp assets/icon.png "$ICON_DIR/nocturne.png" 2>/dev/null || sudo cp icon.png "$ICON_DIR/nocturne.png" 2>/dev/null
sudo cp nocturne.desktop "$DESKTOP_DIR/"
sudo chmod +x "$INSTALL_DIR/desktopApp-thin.jar" 2>/dev/null || true
echo "Installation complete. Launch from applications menu or run: java -jar $INSTALL_DIR/desktopApp-thin.jar"
