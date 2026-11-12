# ĐẶC TẢ NGHIỆP VỤ WEB CÁ NHÂN (SPECIFICATIONS)
> *Dùng làm ngữ cảnh (Context) gửi cho AI để sinh Test Plan & Test Case chính xác theo dự án riêng của bạn.*

---

## 1. THÔNG TIN DỰ ÁN
* **Tên đề tài:** GameNest – Mạng xã hội cộng đồng game thủ (tìm đồng đội, Q&A, chat, team).
* **Công nghệ:** Java 17, Jakarta Servlet 6.0 (JSP/Servlet thuần, không dùng Spring), SQL Server (JDBC), BCrypt, Jakarta Mail (OTP qua Gmail SMTP), Cloudinary (lưu avatar).
* **URL chạy local:** `http://localhost:8080/GameNest` (deploy WAR trên Tomcat 10+).
* **Phạm vi được chọn để kiểm thử trong assignment này** (trong hàng chục module của GameNest, chỉ chọn 3 luồng theo đúng yêu cầu Y2-Y4 của đề bài):
  1. **Đăng ký tài khoản (có xác thực OTP qua email).**
  2. **Đăng nhập.**
  3. **CRUD Game (vai trò Admin)** — module đóng vai trò "CRUD sản phẩm" theo mẫu đề bài.

## 2. QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

### 2.1 Chức năng Đăng ký (`/register` → `/register/verify`)
Đăng ký gồm 2 bước: (1) nhập thông tin + validate + gửi OTP; (2) nhập OTP để tạo account thật.

* `username`: bắt buộc, khớp regex `^[a-zA-Z0-9_]{3,50}$` (chỉ chữ, số, gạch dưới, 3-50 ký tự), không được trùng với username đã tồn tại.
* `email`: bắt buộc, đúng định dạng email, tối đa 255 ký tự, không được trùng với email đã tồn tại.
* `password`: bắt buộc, tối thiểu **8 ký tự** (không yêu cầu ký tự đặc biệt trong code thực tế).
* `displayName`: không bắt buộc, tối đa 100 ký tự; nếu bỏ trống sẽ lấy mặc định = `username`.
* Sau khi validate hợp lệ và không trùng, hệ thống gửi **OTP 6 chữ số**, hiệu lực **5 phút**, tối đa **5 lần thử sai** (cấu hình được qua System Settings) tới email đã nhập.
* Account chỉ thực sự được tạo trong DB sau khi xác thực đúng OTP ở bước verify; nếu OTP sai/hết hạn/hết số lần thử → báo lỗi, không tạo account.

### 2.2 Chức năng Đăng nhập (`/login`)
* Nhập `identifier` (username **hoặc** email) + `password`.
* Sai tài khoản hoặc sai mật khẩu → báo lỗi chung (không tiết lộ username có tồn tại hay không), ở lại trang đăng nhập.
* Tài khoản bị khóa/vô hiệu (status khác ACTIVE) → báo lỗi trạng thái tài khoản, không cho đăng nhập.
* Đăng nhập đúng → tạo session mới (hủy session cũ để chống session fixation), điều hướng theo vai trò:
  * `ADMIN` → `/admin/dashboard`
  * `MODERATOR` → `/moderator/dashboard`
  * `MEMBER` (còn lại) → `/account/home.jsp`

### 2.3 Chức năng CRUD Game (Admin) (`/admin/games/...`)
* **Thêm mới** (`/admin/games/new`):
  * `name`: bắt buộc, không để trống, tối đa 150 ký tự, **không được trùng tên** game đã có (`DuplicateGameException`).
  * `description`: không bắt buộc.
  * `coverImageUrl`: không bắt buộc, tối đa 500 ký tự.
  * `releaseDate`: không bắt buộc; nếu nhập phải là ngày hợp lệ, trong khoảng từ `1950-01-01` đến `hôm nay + 5 năm`.
  * `status`: nếu bỏ trống mặc định là `ACTIVE`; nếu nhập phải là `ACTIVE` hoặc `INACTIVE`.
* **Sửa** (`/admin/games/edit`): cùng rule validate `name`/`description`/`coverImageUrl`/`releaseDate` như thêm mới; **không cho đổi `status` qua form sửa** (status đổi riêng qua chức năng kích hoạt/vô hiệu hóa).
* **Kích hoạt / Vô hiệu hóa** (`/admin/games/status`, tương đương "xóa mềm"): đổi `status` giữa `ACTIVE` ↔ `INACTIVE`. Game không tồn tại → báo lỗi "Game không tồn tại.".
* Game ở trạng thái `INACTIVE` sẽ **không hiển thị** ở trang danh sách/chi tiết Game phía người dùng thường (coi như không tồn tại).
