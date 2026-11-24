package cemestre.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PrologResultLoader {

    private PrologResultLoader() {
    }

    public static JsonObject loadRoot(Path file) throws DataLoadException {
        if (file == null || !Files.exists(file)) {
            throw new DataLoadException("no existe el archivo " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new DataLoadException("la ruta no es un archivo: " + file);
        }
        String text = readUtf8(file);
        return JsonObject.root(JsonParser.parse(text));
    }

    static String readUtf8(Path file) throws DataLoadException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new DataLoadException("no se pudo leer el archivo " + file + ": " + e.getMessage(), e);
        }
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
            // Algunos editores de Windows agregan una marca al inicio, con esto se ignora.
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw new DataLoadException("el archivo " + file + " no esta codificado en UTF-8", e);
        }
    }
}
