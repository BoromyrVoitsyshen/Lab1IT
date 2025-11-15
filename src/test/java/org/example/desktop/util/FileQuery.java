package org.example.desktop.util;

import org.example.desktop.model.RemoteFileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileQueryTest {

    private RemoteFileMetadata fileC;
    private RemoteFileMetadata fileJpg;
    private RemoteFileMetadata fileKt;

    private RemoteFileMetadata fileOldest; 
    private RemoteFileMetadata fileMiddle; 
    private RemoteFileMetadata fileNewest; 

    private List<RemoteFileMetadata> allFilesForFiltering;
    private List<RemoteFileMetadata> allFilesForSorting;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        fileC = createMockFile("source.c", now);
        fileJpg = createMockFile("picture.jpg", now);
        fileKt = createMockFile("script.kt", now);

        allFilesForFiltering = List.of(fileC, fileJpg, fileKt);

        fileOldest = createMockFile("file1.txt", Instant.now().minus(1, ChronoUnit.DAYS));
        fileMiddle = createMockFile("file2.txt", Instant.now().minus(1, ChronoUnit.HOURS));
        fileNewest = createMockFile("file3.txt", Instant.now());

        allFilesForSorting = List.of(fileMiddle, fileNewest, fileOldest);
    }

    @Test
    void testFilterByType_ShouldReturnOnlyCFiles() {
        List<RemoteFileMetadata> result = FileQuery.filterByType(allFilesForFiltering, FileQuery.TypeFilter.C);

        assertThat(result)
                .hasSize(1)
                .containsExactly(fileC);
    }

    @Test
    void testFilterByType_ShouldReturnOnlyJpgFiles() {
        List<RemoteFileMetadata> result = FileQuery.filterByType(allFilesForFiltering, FileQuery.TypeFilter.JPG);

        assertThat(result)
                .hasSize(1)
                .containsExactly(fileJpg);
    }

    @Test
    void testFilterByType_ShouldReturnAllFiles() {
        List<RemoteFileMetadata> result = FileQuery.filterByType(allFilesForFiltering, FileQuery.TypeFilter.ALL);

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder(fileC, fileJpg, fileKt);
    }

    @Test
    void testSortByCreationDate_Ascending() {
        List<RemoteFileMetadata> result = FileQuery.sortByCreationDate(allFilesForSorting, true);

        assertThat(result).containsExactly(fileOldest, fileMiddle, fileNewest);
    }

    @Test
    void testSortByCreationDate_Descending() {
        List<RemoteFileMetadata> result = FileQuery.sortByCreationDate(allFilesForSorting, false);

        assertThat(result).containsExactly(fileNewest, fileMiddle, fileOldest);
    }


    private RemoteFileMetadata createMockFile(String name, Instant createdAt) {
        return new RemoteFileMetadata(
                java.util.UUID.randomUUID().toString(), 
                name,
                createdAt,
                createdAt,
                "demo",
                "demo",
                100L 
        );
    }
}