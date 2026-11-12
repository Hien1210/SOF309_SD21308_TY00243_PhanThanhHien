package com.poly.sof309.controllers;

import com.gamenest.dao.GameDAO;
import com.gamenest.exception.DuplicateGameException;
import com.gamenest.exception.GameNotFoundException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Game;
import com.gamenest.model.GameStatus;
import com.gamenest.service.GameService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit test cho CRUD Game (Admin) trong GameService, ứng với các test
 * case TC_GAME_01..10 trong docs/TEST_CASES.md.
 *
 * TC_GAME_05 (sai định dạng chuỗi ngày, ví dụ "32/13/2024") không unit test ở
 * đây vì việc parse String -> LocalDate nằm trong AdminGameCreateServlet,
 * không thuộc GameService (Service chỉ nhận LocalDate đã parse); sẽ được
 * kiểm thử bằng Selenium (Y4). TC_GAME_11 (phân quyền truy cập) thuộc Filter,
 * cũng để Selenium kiểm thử.
 */
class GameServiceTest {

    private GameDAO gameDAO;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameDAO = mock(GameDAO.class);
        gameService = new GameService(gameDAO);
    }

    @Test
    @DisplayName("TC_GAME_01: Thêm game mới thành công")
    void createGame_validData_returnsCreatedGame() throws Exception {
        when(gameDAO.insert(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            game.setGameId(1);
            return game;
        });

        Game created = gameService.createGame("Valorant Test", "Mô tả", null,
                LocalDate.of(2024, 1, 1), null);

        assertEquals("Valorant Test", created.getName());
        assertEquals(GameStatus.ACTIVE, created.getStatus(), "Status mặc định phải là ACTIVE khi bỏ trống");
    }

    @Test
    @DisplayName("TC_GAME_02: Thêm game với tên để trống")
    void createGame_blankName_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                gameService.createGame("", null, null, null, null));
    }

    @Test
    @DisplayName("TC_GAME_03: Thêm game với tên trùng")
    void createGame_duplicateName_throwsDuplicateGameException() throws Exception {
        when(gameDAO.insert(any(Game.class))).thenThrow(new DuplicateGameException("Tên game đã tồn tại."));

        assertThrows(DuplicateGameException.class, () ->
                gameService.createGame("Game Đã Tồn Tại", null, null, null, null));
    }

    @ParameterizedTest(name = "TC_GAME_04: tên game dài {0} ký tự")
    @DisplayName("TC_GAME_04: Giá trị biên độ dài tên game (150 ký tự)")
    @ValueSource(ints = {150, 151})
    void createGame_nameLengthBoundary(int length) throws Exception {
        String name = "A".repeat(length);
        when(gameDAO.insert(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        if (length <= 150) {
            assertDoesNotThrow(() -> gameService.createGame(name, null, null, null, null));
        } else {
            assertThrows(ValidationException.class, () -> gameService.createGame(name, null, null, null, null));
        }
    }

    @Test
    @DisplayName("TC_GAME_06a: Ngày phát hành trước 1950-01-01")
    void createGame_releaseDateTooOld_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                gameService.createGame("Old Game", null, null, LocalDate.of(1900, 1, 1), null));
    }

    @Test
    @DisplayName("TC_GAME_06b: Ngày phát hành quá xa trong tương lai (> hôm nay + 5 năm)")
    void createGame_releaseDateTooFarInFuture_throwsValidationException() {
        LocalDate tooFar = LocalDate.now().plusYears(6);
        assertThrows(ValidationException.class, () ->
                gameService.createGame("Future Game", null, null, tooFar, null));
    }

    @Test
    @DisplayName("TC_GAME_07: Sửa thông tin game thành công")
    void updateGame_validData_returnsUpdatedGame() throws Exception {
        Game existing = new Game();
        existing.setGameId(5);
        existing.setName("Old Name");
        existing.setStatus(GameStatus.ACTIVE);
        when(gameDAO.findById(5)).thenReturn(Optional.of(existing));

        Game updated = gameService.updateGame(5, "New Name", "Mô tả mới", null, null);

        assertEquals("New Name", updated.getName());
        assertEquals("Mô tả mới", updated.getDescription());
        verify(gameDAO).update(existing);
    }

    @Test
    @DisplayName("TC_GAME_08: Sửa game không tồn tại")
    void updateGame_gameNotFound_throwsGameNotFoundException() throws Exception {
        when(gameDAO.findById(999999)).thenReturn(Optional.empty());

        assertThrows(GameNotFoundException.class, () ->
                gameService.updateGame(999999, "Any Name", null, null, null));
    }

    @Test
    @DisplayName("TC_GAME_09: Vô hiệu hóa game đang ACTIVE")
    void deactivateGame_existingGame_updatesStatusToInactive() throws Exception {
        when(gameDAO.updateStatus(5, GameStatus.INACTIVE)).thenReturn(1);

        assertDoesNotThrow(() -> gameService.deactivateGame(5));
        verify(gameDAO).updateStatus(5, GameStatus.INACTIVE);
    }

    @Test
    @DisplayName("TC_GAME_10: Kích hoạt lại game đang INACTIVE")
    void activateGame_existingGame_updatesStatusToActive() throws Exception {
        when(gameDAO.updateStatus(5, GameStatus.ACTIVE)).thenReturn(1);

        assertDoesNotThrow(() -> gameService.activateGame(5));
        verify(gameDAO).updateStatus(5, GameStatus.ACTIVE);
    }

    @Test
    @DisplayName("TC_GAME_10b: Kích hoạt game không tồn tại báo lỗi")
    void activateGame_gameNotFound_throwsGameNotFoundException() throws Exception {
        when(gameDAO.updateStatus(999999, GameStatus.ACTIVE)).thenReturn(0);

        assertThrows(GameNotFoundException.class, () -> gameService.activateGame(999999));
    }
}
