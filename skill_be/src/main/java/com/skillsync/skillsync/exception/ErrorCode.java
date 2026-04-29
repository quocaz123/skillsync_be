package com.skillsync.skillsync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_EXISTS(409, "Người dùng đã tồn tại", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(401, "Mật khẩu không hợp lệ", HttpStatus.UNAUTHORIZED),
    NO_REFRESH_TOKEN(401, "Không tìm thấy refresh token", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(401, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(401, "Không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Từ chối truy cập", HttpStatus.FORBIDDEN),
    UNVERIFIED_ACCOUNT(403, "Tài khoản chưa được xác minh. Vui lòng xác minh email của bạn.", HttpStatus.FORBIDDEN),
    INVALID_OTP(400, "Mã xác nhận không hợp lệ", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(400, "Mã xác nhận đã hết hạn", HttpStatus.BAD_REQUEST),
    NOT_FOUND(404, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(500, "Lỗi hệ thống nội bộ", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(400, "Tham số yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_GOOGLE_TOKEN(401, "Google token không hợp lệ", HttpStatus.UNAUTHORIZED),
    SLOT_ALREADY_BOOKED(409, "Khung giờ này không còn trống", HttpStatus.CONFLICT),
    SLOT_TIME_CONFLICT(409, "Khung giờ bị trùng với một khung giờ đã có", HttpStatus.CONFLICT),
    /** Giờ kết thúc phải sau giờ bắt đầu (cùng ngày). */
    SLOT_INVALID_TIME_RANGE(400, "Giờ kết thúc phải sau giờ bắt đầu.", HttpStatus.BAD_REQUEST),
    /** Thời điểm bắt đầu slot phải nằm trong tương lai. */
    SLOT_IN_THE_PAST(400, "Không thể tạo slot trong quá khứ.", HttpStatus.BAD_REQUEST),
    SESSION_TIME_CONFLICT(409, "Khung giờ này bị trùng với một buổi học hiện có của bạn", HttpStatus.CONFLICT),
    INSUFFICIENT_CREDITS(402, "Số dư credits không đủ", HttpStatus.PAYMENT_REQUIRED),
    TOO_EARLY_TO_JOIN(400, "Buổi học chưa bắt đầu (chỉ được tham gia trước 10 phút)", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED(400, "Đã hết thời gian tham gia buổi học", HttpStatus.BAD_REQUEST),
    SKILL_NOT_FOUND(404, "Không tìm thấy kỹ năng giảng dạy", HttpStatus.NOT_FOUND),
    /** Đã có bản ghi dạy cùng skill + level — không tạo trùng. */
    TEACHING_SKILL_DUPLICATE(409, "Bạn đã đăng ký dạy kỹ năng này ở level này rồi.", HttpStatus.CONFLICT),
    TEACHING_SKILL_NOT_ACCEPTING(403, "Kỹ năng giảng dạy này đang tạm dừng và không nhận lịch hẹn mới", HttpStatus.FORBIDDEN),
    SESSION_ALREADY_DECIDED(409, "Buổi học này đã được chấp nhận hoặc từ chối trước đó", HttpStatus.CONFLICT),
    NOT_SESSION_TEACHER(403, "Bạn không phải là Mentor của buổi học này", HttpStatus.FORBIDDEN),
    
    // Nâng cấp / Bổ sung ErrorCode mới:
    MISSION_ALREADY_COMPLETED(409, "Bạn đã hoàn thành nhiệm vụ này rồi.", HttpStatus.CONFLICT),
    MISSION_REQUIREMENTS_NOT_MET(400, "Chưa hoàn thành yêu cầu của nhiệm vụ.", HttpStatus.BAD_REQUEST),
    LEARNING_PATH_NOT_READY(400, "Lộ trình chưa sẵn sàng để đăng ký.", HttpStatus.BAD_REQUEST),
    LEARNING_PATH_ALREADY_RATED(409, "Bạn đã đánh giá lộ trình này rồi.", HttpStatus.CONFLICT),
    CATEGORY_ALREADY_EXISTS(409, "Danh mục diễn đàn đã tồn tại.", HttpStatus.CONFLICT),
    FILE_UPLOAD_FAILED(500, "Lỗi khi xử lý file upload.", HttpStatus.INTERNAL_SERVER_ERROR),
    EVIDENCE_NOT_FOUND(404, "Không tìm thấy minh chứng.", HttpStatus.NOT_FOUND),
    TEACHING_SKILL_NOT_FOUND(404, "Không tìm thấy kỹ năng giảng dạy.", HttpStatus.NOT_FOUND)
    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
