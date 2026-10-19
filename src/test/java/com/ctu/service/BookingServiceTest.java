
package com.ctu.service;

import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.exception.PaymentFailedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    RoomRepository roomRepository;

    @Mock
    PaymentGateway paymentGateway;

    @InjectMocks
    BookingService bookingService;

    Room room;

    @BeforeEach
    void setUp() {
        room = new Room("R101", 500000, true);
    }

    // 1. Đặt phòng thành công
    @Test
    void testBookRoom_Success() {
        when(roomRepository.findById("R101"))
                .thenReturn(Optional.<Room>of(room));

        when(paymentGateway.charge("123456", 1000000))
                .thenReturn(true);

        bookingService.bookRoom("R101", 2, "123456");

        assertFalse(room.isAvailable());

        verify(paymentGateway, times(1))
                .charge("123456", 1000000);

        verify(roomRepository, times(1)).save(room);
    }

    // 2. Không tìm thấy phòng
    @Test
    void testBookRoom_RoomNotFound_ThrowsException() {
        when(roomRepository.findById("R101"))
                .thenReturn(Optional.<Room>empty());

        assertThrows(RoomNotAvailableException.class, () -> {
            bookingService.bookRoom("R101", 1, "123456");
        });
    }

    // 3. Phòng đã được đặt
    @Test
    void testBookRoom_RoomAlreadyBooked_ThrowsException() {
        room.setAvailable(false);

        when(roomRepository.findById("R101"))
                .thenReturn(Optional.<Room>of(room));

        assertThrows(RoomNotAvailableException.class, () -> {
            bookingService.bookRoom("R101", 1, "123456");
        });
    }

    // 4. Thanh toán thất bại
    @Test
    void testBookRoom_PaymentFailed_ThrowsException() {
        when(roomRepository.findById("R101"))
                .thenReturn(Optional.<Room>of(room));

        when(paymentGateway.charge("123456", 500000))
                .thenReturn(false);

        assertThrows(PaymentFailedException.class, () -> {
            bookingService.bookRoom("R101", 1, "123456");
        });

        assertTrue(room.isAvailable());

        verify(roomRepository, never()).save(any(Room.class));
    }

    // 5. Số đêm không hợp lệ
    @Test
    void testBookRoom_InvalidNights_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 0, "123456");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", -1, "123456");
        });
    }

    // 6. Thẻ thanh toán null hoặc rỗng
    @Test
    void testBookRoom_NullOrEmptyCreditCard_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 1, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.bookRoom("R101", 1, "");
        });
    }
}
