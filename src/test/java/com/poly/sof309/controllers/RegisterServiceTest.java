package com.poly.sof309.controllers;

import com.gamenest.dao.AccountDAO;
import com.gamenest.dto.PendingRegistration;
import com.gamenest.exception.DuplicateAccountException;
import com.gamenest.exception.ValidationException;
import com.gamenest.model.Account;
import com.gamenest.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * [Y3] Unit test cho logic đăng ký trong AccountService#prepareRegistration,
 * ứng với các test case TC_REG_01..08 trong docs/TEST_CASES.md.
 *
 * TC_REG_09/10 (xác thực OTP sai/hết hạn) không được unit test ở đây: OtpService
 * tự khởi tạo SystemSettingsService/kết nối DB thật bên trong, không thể inject
 * mock qua constructor hiện có, nên sẽ được kiểm thử bằng Selenium (Y4) thay vì
 * JUnit cách ly.
 */
class RegisterServiceTest {

    private AccountDAO accountDAO;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountDAO = mock(AccountDAO.class);
        accountService = new AccountService(accountDAO);
    }

    @Test
    @DisplayName("TC_REG_01: Đăng ký thành công với dữ liệu hợp lệ")
    void prepareRegistration_validData_returnsPendingRegistration() throws Exception {
        when(accountDAO.findByUsername("hien_test01")).thenReturn(Optional.empty());
        when(accountDAO.findByEmail("hien_test01@gmail.com")).thenReturn(Optional.empty());

        PendingRegistration pending = accountService.prepareRegistration(
                "hien_test01", "hien_test01@gmail.com", "Password123", "Hien Test");

        assertEquals("hien_test01", pending.getUsername());
        assertEquals("hien_test01@gmail.com", pending.getEmail());
        assertEquals("Hien Test", pending.getDisplayName());
        assertNotEquals("Password123", pending.getPasswordHash(), "Password phải được băm (BCrypt), không lưu plaintext");
    }

    @Test
    @DisplayName("TC_REG_01b: Bỏ trống displayName sẽ dùng mặc định = username")
    void prepareRegistration_blankDisplayName_fallsBackToUsername() throws Exception {
        when(accountDAO.findByUsername("hien_test02")).thenReturn(Optional.empty());
        when(accountDAO.findByEmail("hien_test02@gmail.com")).thenReturn(Optional.empty());

        PendingRegistration pending = accountService.prepareRegistration(
                "hien_test02", "hien_test02@gmail.com", "Password123", "");

        assertEquals("hien_test02", pending.getDisplayName());
    }

    @Test
    @DisplayName("TC_REG_02: Username quá ngắn (< 3 ký tự)")
    void prepareRegistration_usernameTooShort_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                accountService.prepareRegistration("ab", "valid@gmail.com", "Password123", null));
    }

    @Test
    @DisplayName("TC_REG_03: Username chứa ký tự đặc biệt không hợp lệ")
    void prepareRegistration_usernameInvalidCharacters_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                accountService.prepareRegistration("hien@test", "valid@gmail.com", "Password123", null));
    }

    @Test
    @DisplayName("TC_REG_04: Username đã tồn tại")
    void prepareRegistration_duplicateUsername_throwsDuplicateAccountException() throws Exception {
        when(accountDAO.findByUsername("existing_user")).thenReturn(Optional.of(new Account()));

        assertThrows(DuplicateAccountException.class, () ->
                accountService.prepareRegistration("existing_user", "new@gmail.com", "Password123", null));
    }

    @Test
    @DisplayName("TC_REG_05: Email không đúng định dạng")
    void prepareRegistration_invalidEmailFormat_throwsValidationException() {
        assertThrows(ValidationException.class, () ->
                accountService.prepareRegistration("valid_user", "hien_gmail.com", "Password123", null));
    }

    @Test
    @DisplayName("TC_REG_06: Email đã tồn tại")
    void prepareRegistration_duplicateEmail_throwsDuplicateAccountException() throws Exception {
        when(accountDAO.findByUsername("new_user")).thenReturn(Optional.empty());
        when(accountDAO.findByEmail("existing@gmail.com")).thenReturn(Optional.of(new Account()));

        assertThrows(DuplicateAccountException.class, () ->
                accountService.prepareRegistration("new_user", "existing@gmail.com", "Password123", null));
    }

    @ParameterizedTest(name = "TC_REG_07/08: password dài {0} ký tự -> hợp lệ={1}")
    @DisplayName("TC_REG_07/08: Giá trị biên độ dài password (8 ký tự)")
    @CsvSource({
            "Pass123, false",   // 7 ký tự - dưới ngưỡng tối thiểu
            "Pass1234, true"    // 8 ký tự - đúng ngưỡng tối thiểu
    })
    void prepareRegistration_passwordLengthBoundary(String password, boolean expectedValid) throws Exception {
        when(accountDAO.findByUsername(anyString())).thenReturn(Optional.empty());
        when(accountDAO.findByEmail(anyString())).thenReturn(Optional.empty());

        if (expectedValid) {
            assertDoesNotThrow(() ->
                    accountService.prepareRegistration("boundary_user", "boundary@gmail.com", password, null));
        } else {
            assertThrows(ValidationException.class, () ->
                    accountService.prepareRegistration("boundary_user", "boundary@gmail.com", password, null));
        }
    }
}
