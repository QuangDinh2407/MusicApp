package com.ck.music_app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.ck.music_app.Model.SearchHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "history_list";
    private static final int MAX_HISTORY_SIZE = 20;

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Thêm từ khóa tìm kiếm vào lịch sử
    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty())
            return;

        List<SearchHistory> historyList = getSearchHistory();

        // Xóa từ khóa cũ nếu đã tồn tại
        for (int i = historyList.size() - 1; i >= 0; i--) {
            if (historyList.get(i).getQuery().equalsIgnoreCase(query.trim())) {
                historyList.remove(i);
            }
        }

        // Thêm từ khóa mới vào đầu danh sách
        SearchHistory newHistory = new SearchHistory(query.trim(), System.currentTimeMillis(), "query");
        historyList.add(0, newHistory);

        // Giới hạn số lượng lịch sử
        if (historyList.size() > MAX_HISTORY_SIZE) {
            historyList = historyList.subList(0, MAX_HISTORY_SIZE);
        }

        saveSearchHistory(historyList);
    }

    // Lấy danh sách lịch sử tìm kiếm
    public List<SearchHistory> getSearchHistory() {
        String json = sharedPreferences.getString(KEY_HISTORY, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type listType = new TypeToken<List<SearchHistory>>() {
            }.getType();
            List<SearchHistory> historyList = gson.fromJson(json, listType);

            if (historyList == null) {
                return new ArrayList<>();
            }

            // Sắp xếp theo thời gian (mới nhất trước)
            Collections.sort(historyList, new Comparator<SearchHistory>() {
                @Override
                public int compare(SearchHistory h1, SearchHistory h2) {
                    return Long.compare(h2.getTimestamp(), h1.getTimestamp());
                }
            });

            return historyList;
        } catch (Exception e) {
            // Nếu có lỗi parse JSON, trả về list rỗng
            return new ArrayList<>();
        }
    }

    // Lấy danh sách tìm kiếm gần đây (5 item gần nhất)
    public List<SearchHistory> getRecentSearches() {
        List<SearchHistory> allHistory = getSearchHistory();
        int size = Math.min(allHistory.size(), 5);
        return allHistory.subList(0, size);
    }

    // Xóa một item khỏi lịch sử
    public void removeSearchHistory(String query) {
        List<SearchHistory> historyList = getSearchHistory();
        for (int i = historyList.size() - 1; i >= 0; i--) {
            if (historyList.get(i).getQuery().equalsIgnoreCase(query)) {
                historyList.remove(i);
            }
        }
        saveSearchHistory(historyList);
    }

    // Xóa toàn bộ lịch sử
    public void clearSearchHistory() {
        sharedPreferences.edit().remove(KEY_HISTORY).apply();
    }

    // Lưu danh sách lịch sử
    private void saveSearchHistory(List<SearchHistory> historyList) {
        String json = gson.toJson(historyList);
        sharedPreferences.edit().putString(KEY_HISTORY, json).apply();
    }

    // Kiểm tra xem có lịch sử không
    public boolean hasSearchHistory() {
        return !getSearchHistory().isEmpty();
    }
}