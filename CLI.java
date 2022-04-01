package advisor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLI {
    private static final Scanner scanner = new Scanner(System.in);
    private final App app;
    private final View view;
    private final Parameters params;
    private List<Entry> currentList = List.of();
    private int currentPage;
    private int totalPages;

    CLI(Parameters params) {
        this.params = params;
        this.app = new App(params);
        this.view = new View();
    }

    void showMainMenu() {

        while (true) {
            String input = scanner.nextLine().strip().toLowerCase();

            if ("exit".equals(input)) {
                if (currentList.isEmpty()) {
                    quit();
                    break;
                } else {
                    currentList = List.of();
                }
            } else if ("featured".equals(input)) {
                showFeatured();
            } else if ("new".equals(input)) {
                showNew();
            } else if ("categories".equals(input)) {
                showCategories();
            } else if ("auth".equals(input)) {
                app.auth();
            } else if ("next".equals(input)) {
                getNextPage();
            } else if ("prev".equals(input)) {
                getPrevPage();
            } else {
                Pattern pattern = Pattern.compile("playlists (.+)");
                Matcher matcher = pattern.matcher(input);
                if (matcher.matches()) {
                    showPlaylists(matcher.group(1));
                }
            }
        }
    }

    private void quit() {
        try {
            app.stopServer();
        } catch (Exception ignored) {}

        System.out.println("---GOODBYE!---");
    }

    private void showFeatured() {
        updateCurrentList(app.getFeaturedPlaylists());
        showEntries(getPage());
    }

    private void showNew() {
        updateCurrentList(app.getNewReleases());
        showEntries(getPage());
    }

    private void showCategories() {
        updateCurrentList(app.getCategories());
        showEntries(getPage());
    }

    private void showPlaylists(String category) {
        updateCurrentList(app.getPlaylist(category));
        showEntries(getPage());
    }

    private void getNextPage() {
        showEntries(getPage(currentPage + 1));
    }

    private void getPrevPage() {
        showEntries(getPage(currentPage - 1));
    }

    private List<Entry> getPage() {
        int startIndex = params.entriesPerPage * (currentPage - 1);
        return currentList.subList(startIndex,
                Math.min(startIndex + params.entriesPerPage, currentList.size()));
    }

    private List<Entry> getPage(int pageNum) {
        if (pageNum < 1 || pageNum > totalPages) {
            return List.of();
        }
        currentPage = pageNum;
        return getPage();
    }

    private void updateCurrentList(List<Entry> list) {
        currentList = list;
        currentPage = 1;
        updateTotalPages();
    }

    private void updateTotalPages() {
        totalPages = (int) Math.ceil((double) currentList.size() / params.entriesPerPage);
    }

    private void showEntries(List<Entry> data) {
        view.showEntries(data, currentPage, totalPages);
    }
}
