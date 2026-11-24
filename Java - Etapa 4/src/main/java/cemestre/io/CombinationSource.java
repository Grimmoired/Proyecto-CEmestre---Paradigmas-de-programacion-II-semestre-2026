package cemestre.io;

import cemestre.model.CombinationSet;

import java.nio.file.Path;

public interface CombinationSource {
    CombinationSet load(Path file) throws DataLoadException;
}
