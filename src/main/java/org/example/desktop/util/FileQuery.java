package org.example.desktop.util;

import org.example.desktop.model.RemoteFileMetadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class FileQuery {
    private FileQuery() {}
    public enum TypeFilter { ALL, C, JPG }

    public static List<RemoteFileMetadata> filterByType(List<RemoteFileMetadata> list, TypeFilter filter) {
        if (filter == null || filter == TypeFilter.ALL) {
            return new ArrayList<>(list);
        }
        String neededExtension;
        switch (filter) {
            case C:
                neededExtension = "c";
                break;
            case JPG:
                neededExtension = "jpg";
                break;
            default:
                return new ArrayList<>(list);
        }

        return list.stream()
                .filter(m -> neededExtension.equals(m.getExtensionLower()))
                .collect(Collectors.toList());
    }
    public static List<RemoteFileMetadata> sortByCreationDate(List<RemoteFileMetadata> list, boolean ascending) {
        Comparator<RemoteFileMetadata> cmp = Comparator.comparing(RemoteFileMetadata::getCreatedAt);

        if (!ascending) {
            cmp = cmp.reversed();
        }

        return list.stream().sorted(cmp).collect(Collectors.toList());
    }
}