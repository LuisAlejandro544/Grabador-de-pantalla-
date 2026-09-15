#!/usr/bin/env bash
# ==============================================================================
# Script de Generación de Firma Debug Obligatoria desde Cero
# ==============================================================================
# Este script fuerza la creación de un nuevo keystore debug completamente nuevo,
# eliminando cualquier firma preexistente y sin requerir interacción del usuario.
# Está diseñado para ejecutarse localmente o dentro del flujo de GitHub Actions.
# ==============================================================================

set -euo pipefail

# Directorio destino del keystore (por defecto directorio actual o el proporcionado por parámetro)
TARGET_DIR="${1:-.}"
KEYSTORE_FILE="$TARGET_DIR/debug.keystore"
STORE_PASS="android"
KEY_PASS="android"
KEY_ALIAS="androiddebugkey"
CERT_DNAME="CN=Android Debug,OU=Dev,O=OpenSource,L=Mobile,ST=Dev,C=ES"

echo "============================================================"
echo "  GENERADOR DE FIRMA DEBUG DESDE CERO (NON-INTERACTIVE)"
echo "============================================================"

# Crear directorio si no existe
mkdir -p "$TARGET_DIR"

# Forzar eliminación de cualquier firma previa existente para garantizar que sea 100% nueva
if [ -f "$KEYSTORE_FILE" ]; then
    echo "[!] Se detectó una firma previa en $KEYSTORE_FILE. Eliminando para forzar generación desde cero..."
    rm -f "$KEYSTORE_FILE"
fi

echo "[*] Generando nueva clave RSA 2048 bits para depuración..."
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STORE_PASS" \
    -alias "$KEY_ALIAS" \
    -keypass "$KEY_PASS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "$CERT_DNAME" \
    -noprompt

# También asegurar copia en ~/.android/debug.keystore para compatibilidad con herramientas estándar
mkdir -p "$HOME/.android"
cp -f "$KEYSTORE_FILE" "$HOME/.android/debug.keystore"

echo "[✓] ¡Firma debug generada exitosamente!"
echo "    Ubicación principal: $KEYSTORE_FILE"
echo "    Ubicación estándar:  $HOME/.android/debug.keystore"
echo "    Alias: $KEY_ALIAS"
echo "    Validez: 10000 días"
ls -lh "$KEYSTORE_FILE"
echo "============================================================"
