package me.n1ar4.jar.analyzer.core;

import me.n1ar4.jar.analyzer.engine.model.MethodView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseManagerMethodStateTest {
    @AfterEach
    void cleanup() {
        DatabaseManager.clearAllData();
    }

    @Test
    void favoritesShouldDeduplicateAndRespectCapacity() {
        for (int i = 0; i < 300; i++) {
            DatabaseManager.addFav(method(i));
        }

        List<MethodView> favorites = DatabaseManager.getAllFavMethods();
        assertEquals(256, favorites.size());
        assertEquals("demo/Fav44", favorites.get(0).getClassName());
        assertEquals("demo/Fav299", favorites.get(favorites.size() - 1).getClassName());

        DatabaseManager.addFav(method(100));

        favorites = DatabaseManager.getAllFavMethods();
        assertEquals(256, favorites.size());
        assertEquals(1L, favorites.stream().filter(item -> "demo/Fav100".equals(item.getClassName())).count());
        assertEquals("demo/Fav100", favorites.get(favorites.size() - 1).getClassName());
    }

    @Test
    void historiesShouldDeduplicateAndRespectCapacity() {
        for (int i = 0; i < 600; i++) {
            DatabaseManager.insertHistory(method(i));
        }

        List<MethodView> histories = DatabaseManager.getAllHisMethods();
        assertEquals(512, histories.size());
        assertEquals("demo/Fav88", histories.get(0).getClassName());
        assertEquals("demo/Fav599", histories.get(histories.size() - 1).getClassName());

        DatabaseManager.insertHistory(method(200));

        histories = DatabaseManager.getAllHisMethods();
        assertEquals(512, histories.size());
        assertEquals(1L, histories.stream().filter(item -> "demo/Fav200".equals(item.getClassName())).count());
        assertEquals("demo/Fav88", histories.get(0).getClassName());
        assertEquals("demo/Fav200", histories.get(histories.size() - 1).getClassName());
    }

    private static MethodView method(int index) {
        MethodView result = new MethodView();
        result.setClassName("demo/Fav" + index);
        result.setMethodName("run");
        result.setMethodDesc("()V");
        result.setJarId(index);
        return result;
    }
}
