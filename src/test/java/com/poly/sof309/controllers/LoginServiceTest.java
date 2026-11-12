package com.poly.sof309.controllers;

import com.gamenest.dao.AccountDAO;
import com.gamenest.exception.AccountStatusException;
import com.gamenest.exception.AuthenticationException;
import com.gamenest.model.Account;
import com.gamenest.model.AccountRole;
import com.gamenest.model.AccountStatus;
import com.gamenest.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit test cho logic đăng nhập trong AccountService#login,
 * ứng với các test case TC_AUTH_01..07 trong docs/TEST_CASES.md.
 *
 * Việc điều hướng theo vai trò (ADMIN -> /admin/dashboard...) nằm ở
 * LoginServlet, không thuộc Service — ở đây chỉ xác nhận login() trả về
 * đúng Account (kèm role) để Servlet điều hướng đúng; luồng điều hướng
 * thật được kiểm thử bằng Selenium (Y4).
 */
class LoginServiceTest {

    private static final String RAW_PASSWORD = "Password123";

    private AccountDAO accountDAO;
    private AccountService accountService;
    private Account activeAccount;

    @BeforeEach
    void setUp() {
        accountDAO = mock(AccountDAO.class);
        accountService = new AccountService(accountDAO);

        activeAccount = new Account();
        activeAccount.setAccountId(1);
        activeAccount.setUsername("member01");
        activeAccount.setEmail("member01@gmail.com");
        activeAccount.setPasswordHash(BCrypt.hashpw(RAW_PASSWORD, BCrypt.gensalt()));
        activeAccount.setStatus(AccountStatus.ACTIVE);
        activeAccount.setRole(AccountRole.USER);
    }

    @Test
    @DisplayName("TC_AUTH_01: Đăng nhập thành công bằng username")
    void login_validUsernameAndPassword_returnsAccount() throws Exception {
        when(accountDAO.findByUsernameOrEmail("member01")).thenReturn(Optional.of(activeAccount));

        Account result = accountService.login("member01", RAW_PASSWORD);

        assertEquals("member01", result.getUsername());
    }

    @Test
    @DisplayName("TC_AUTH_02: Đăng nhập thành công bằng email")
    void login_validEmailAndPassword_returnsAccount() throws Exception {
        when(accountDAO.findByUsernameOrEmail("member01@gmail.com")).thenReturn(Optional.of(activeAccount));

        Account result = accountService.login("member01@gmail.com", RAW_PASSWORD);

        assertEquals("member01@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("TC_AUTH_03: Đăng nhập tài khoản Admin trả về đúng role để Servlet điều hướng")
    void login_adminAccount_returnsAdminRole() throws Exception {
        activeAccount.setRole(AccountRole.ADMIN);
        when(accountDAO.findByUsernameOrEmail("member01")).thenReturn(Optional.of(activeAccount));

        Account result = accountService.login("member01", RAW_PASSWORD);

        assertEquals(AccountRole.ADMIN, result.getRole());
    }

    @Test
    @DisplayName("TC_AUTH_04: Sai mật khẩu")
    void login_wrongPassword_throwsAuthenticationException() throws Exception {
        when(accountDAO.findByUsernameOrEmail("member01")).thenReturn(Optional.of(activeAccount));

        assertThrows(AuthenticationException.class, () ->
                accountService.login("member01", "wrong-password"));
    }

    @Test
    @DisplayName("TC_AUTH_05: Tài khoản không tồn tại")
    void login_accountNotFound_throwsAuthenticationException() throws Exception {
        when(accountDAO.findByUsernameOrEmail("khong_ton_tai")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () ->
                accountService.login("khong_ton_tai", RAW_PASSWORD));
    }

    @Test
    @DisplayName("TC_AUTH_06: Để trống mật khẩu")
    void login_emptyPassword_throwsAuthenticationException() {
        assertThrows(AuthenticationException.class, () ->
                accountService.login("member01", ""));
    }

    @Test
    @DisplayName("TC_AUTH_07: Tài khoản bị khóa (BANNED) không được đăng nhập")
    void login_bannedAccount_throwsAccountStatusException() throws Exception {
        activeAccount.setStatus(AccountStatus.BANNED);
        when(accountDAO.findByUsernameOrEmail("member01")).thenReturn(Optional.of(activeAccount));

        assertThrows(AccountStatusException.class, () ->
                accountService.login("member01", RAW_PASSWORD));
    }
}
