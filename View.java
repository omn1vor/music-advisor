package advisor;

import java.util.List;

public class View {
    public View() {
    }

    public void showEntries(List<Entry> data, int currentPage, int totalPages) {
        if (data.isEmpty()) {
            System.out.println("No more pages.");
            return;
        }
        data.forEach(this::printEntry);

        System.out.printf("---PAGE %d OF %d---%n", currentPage, totalPages);
    }

    private void printEntry(Entry entry) {
        String title = entry.getTitle();
        if (title != null) {
            System.out.println(title);
        }
        List<String> artists = entry.getArtists();
        if (artists != null) {
            System.out.println(artists);
        }
        String url = entry.getURL();
        if (url != null) {
            System.out.println(url);
        }
        if (!entry.isCompact()) {
            System.out.println();
        }
    }



}
