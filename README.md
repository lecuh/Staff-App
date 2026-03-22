# CULINA - Staff Management Application 📱🍽️

Dự án Android dành cho nhân viên nhà hàng, hỗ trợ quản lý Bàn (Tables), Thực đơn (Menu), Đơn hàng (Orders) và Đặt bàn (Reservations).

## 🚀 Yêu cầu hệ thống
* **Android Studio:** Ladybug (hoặc mới hơn)
* **JDK:** 17+
* **Gradle:** 8.x
* **Backend:** Cần chạy Server cục bộ tại cổng `8080`.

## 🛠️ Cài đặt môi trường

### 1. Kết nối Backend
Mặc định ứng dụng kết nối với localhost qua IP dành riêng cho Emulator:
* **Base URL:** `http://10.0.2.2:8080/`
* Đảm bảo Server của bạn đã bật và cho phép các request từ địa chỉ này.

### 2. Cấu hình Emulator Tablet (Khuyến nghị)
Giao diện được tối ưu hóa cho Tablet để sử dụng tính năng **Quick Menu (Split-screen)** khi nhấn vào Bàn. Hãy tạo Emulator theo thông số sau:

1. Vào **Device Manager** trong Android Studio.
2. Chọn **Create Device**.
3. Chọn danh mục **Tablet**.
4. Chọn **Pixel Tablet** hoặc **Nexus 9** (Màn hình lớn).
5. **Quan trọng:** Khi chạy ứng dụng, hãy để máy ở chế độ **Landscape (Nằm ngang)** để hiển thị được đồng thời Menu bên trái và Giỏ hàng bên phải.

## 📱 Các chức năng chính

*   **Tables:** Xem sơ đồ bàn. Bàn trống (Xanh), Có khách (Đỏ). Nhấn vào bàn để mở nhanh Menu.
*   **Quick Order:** Chọn món ngay khi đang xem bàn, nhấn "Send to Kitchen" để tự động đổi trạng thái bàn sang Occupied.
*   **Order Management:** Quản lý trạng thái món ăn (Pending, Preparing, Ready, Served).
*   **Reservations:** Quản lý đặt bàn trực tuyến.
    *   **Tab PENDING:** Gọi điện xác nhận và Confirm.
    *   **Tab CONFIRMED:** Check-in khi khách đến hoặc đánh dấu No-show.
    *   **Tab CHECKED IN:** Theo dõi khách đang ngồi.

## ⚠️ Lưu ý kỹ thuật cho Team

1.  **Lỗi 500 khi cập nhật Status:**
    *   Server yêu cầu Body là một **JSON String** (ví dụ: `"READY"` thay vì `READY`).
    *   Sử dụng `toRequestBody` kèm `"application/json".toMediaTypeOrNull()` khi gọi API PATCH.
2.  **Trạng thái Checked-in:**
    *   Trong code đang gửi query `status=ARRIVED`. Hãy đảm bảo Enum ở Backend khớp với giá trị này.
3.  **Lỗi Redeclaration:**
    *   Nếu gặp lỗi này khi Build, hãy vào `Build > Clean Project` và `Build > Rebuild Project` để xóa cache cũ của IDE.

## 📦 Dependencies chính
*   **Jetpack Compose:** UI Toolkit.
*   **Retrofit & Gson:** Gọi API và chuyển đổi dữ liệu.
*   **Coil:** Load hình ảnh món ăn từ URL.
*   **OkHttp:** Xử lý request body.
