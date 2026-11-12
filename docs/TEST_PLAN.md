# KẾ HOẠCH KIỂM THỬ (TEST PLAN)

* **Dự án:** GameNest – Mạng xã hội cộng đồng game thủ
* **Sinh viên:** Phan Thanh Hiền - SD21308 - TY00243

---

## 1. MỤC TIÊU KIỂM THỬ
* Kiểm thử logic nghiệp vụ (Unit Test với JUnit 5) cho Controller/Service của 3 luồng: **Đăng ký + OTP**, **Đăng nhập**, **CRUD Game (Admin)**.
* Kiểm thử giao diện tự động (Automation UI Test với Selenium WebDriver, mô hình Page Object) cho 3 luồng trên.
* Tích hợp kiểm thử tự động với GitHub Actions CI/CD (chạy JUnit + Selenium headless khi push code).

## 2. PHẠM VI KIỂM THỬ
* **In-Scope:**
  * Đăng ký tài khoản (validate username/email/password, trùng username/email, gửi & xác thực OTP).
  * Đăng nhập (đúng/sai tài khoản-mật khẩu, tài khoản bị khóa, điều hướng theo vai trò).
  * CRUD Game phía Admin (thêm, sửa, kích hoạt/vô hiệu hóa, validate tên trùng/độ dài/ngày phát hành).
* **Out-of-Scope:** Các module còn lại của GameNest (Q&A, LFG, Team, Chat, Report, Notification, Moderator...), kiểm thử hiệu năng (Performance Test), bảo mật chuyên sâu (Penetration Test), gửi email OTP thật (sẽ mock/stub khi viết Unit Test).

## 3. CHIẾN LƯỢC KIỂM THỬ
| Kỹ thuật | Áp dụng cho |
| :--- | :--- |
| `@ParameterizedTest` + `@CsvSource` | Kiểm tra biên `username`/`password`/`email`/`tên game` (độ dài, định dạng) |
| `assertThrows()` | `ValidationException`, `DuplicateAccountException`, `DuplicateGameException`, `AuthenticationException`, `AccountStatusException` |
| Selenium POM (`LoginPage`, `RegisterPage`, `AdminGamePage`) | Luồng UI đăng ký/đăng nhập/CRUD Game |
| TestNG `groups = "smoke"` + `@DataProvider` | Test lặp nhiều bộ tài khoản/dữ liệu game |
| `ITestListener` | Tự động chụp ảnh màn hình khi Selenium test fail |

## 4. MÔI TRƯỜNG KIỂM THỬ
* Local: Windows, Tomcat 10+, SQL Server, Chrome mới nhất.
* CI: GitHub Actions (Ubuntu), Chrome Headless.

## 5. TIÊU CHÍ HOÀN THÀNH
* 100% test script (JUnit + Selenium) thực thi pass.
* Pipeline GitHub Actions báo xanh (Success).
* Đầy đủ minh chứng AI trong `docs/ai-logs/`.
