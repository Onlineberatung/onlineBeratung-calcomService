package com.vi.appointmentservice.service;

import com.vi.appointmentservice.adapters.keycloak.dto.KeycloakLoginResponseDTO;
import com.vi.appointmentservice.api.model.CalcomBooking;
import com.vi.appointmentservice.messageservice.generated.web.MessageControllerApi;
import com.vi.appointmentservice.messageservice.generated.web.model.AliasMessageDTO;
import com.vi.appointmentservice.messageservice.generated.web.model.MessageType;
import com.vi.appointmentservice.model.CalcomBookingToAsker;
import com.vi.appointmentservice.model.CalcomUserToConsultant;
import com.vi.appointmentservice.port.out.IdentityClient;
import com.vi.appointmentservice.repository.CalcomBookingToAskerRepository;
import com.vi.appointmentservice.repository.CalcomUserToConsultantRepository;
import com.vi.appointmentservice.service.securityheader.SecurityHeaderSupplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessagesService {

  private final @NonNull MessageControllerApi messageControllerApi;
  private final @NonNull UserService userService;
  private final @NonNull CalComBookingService calComBookingService;
  private final @NonNull CalcomUserToConsultantRepository calcomUserToConsultantRepository;
  private final @NonNull CalcomBookingToAskerRepository calcomBookingToAskerRepository;
  private final @NonNull IdentityClient identityClient;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  @Value("${keycloakService.technical.username}")
  private String keycloakTechnicalUsername;

  @Value("${keycloakService.technical.password}")
  private String keycloakTechnicalPassword;


  public void publishCancellationMessage(Long bookingId) {
    CalcomBooking booking = calComBookingService.getBookingById(bookingId);
    AliasMessageDTO message = createCancellationMessage(booking);
    sendMessage(booking, message);
  }

  public void publishNewAppointmentMessage(Long bookingId) {
    CalcomBooking booking = calComBookingService.getBookingById(bookingId);
    AliasMessageDTO message = createNewAppointmentMessage(booking);
    sendMessage(booking, message);
  }

  private AliasMessageDTO createNewAppointmentMessage(CalcomBooking booking) {
    AliasMessageDTO message = new AliasMessageDTO();
    JSONObject messageContent = new JSONObject();
    messageContent.append("title", booking.getTitle());
    messageContent.append("startTime", booking.getStartTime());
    messageContent.append("endTime", booking.getEndTime());
    message.setMessageType(MessageType.APPOINTMENT_SET);
    message.setContent(messageContent.toString());
    return message;
  }
  private AliasMessageDTO createCancellationMessage(CalcomBooking booking) {
    AliasMessageDTO message = new AliasMessageDTO();
    JSONObject messageContent = new JSONObject();
    messageContent.append("title", booking.getTitle());
    messageContent.append("startTime", booking.getStartTime());
    messageContent.append("endTime", booking.getEndTime());
    message.setMessageType(MessageType.APPOINTMENT_CANCELLED);
    message.setContent(messageContent.toString());
    return message;
  }

  private void sendMessage(CalcomBooking booking, AliasMessageDTO message) {
    addTechnicalUserHeaders(messageControllerApi.getApiClient());
    messageControllerApi.saveAliasMessageWithContent(getRocketChatGroupId(booking), message);
  }

  private String getRocketChatGroupId(CalcomBooking booking) {
    CalcomUserToConsultant byCalComUserId = calcomUserToConsultantRepository
        .findByCalComUserId(Long.valueOf(booking.getUserId()));
    String consultantId = byCalComUserId.getConsultantId();
    CalcomBookingToAsker byCalcomBookingId = calcomBookingToAskerRepository
        .findByCalcomBookingId(Long.valueOf(booking.getId()));
    String askerId = byCalcomBookingId.getAskerId();
    return userService
        .getRocketChatGroupId(consultantId, askerId);

  }

  private void addTechnicalUserHeaders(
      com.vi.appointmentservice.messageservice.generated.ApiClient apiClient) {
    KeycloakLoginResponseDTO keycloakLoginResponseDTO = identityClient.loginUser(
        keycloakTechnicalUsername, keycloakTechnicalPassword
    );
    log.debug("Technical Acces Token: {}", keycloakLoginResponseDTO.getAccessToken());
    HttpHeaders headers = this.securityHeaderSupplier
        .getKeycloakAndCsrfHttpHeaders(keycloakLoginResponseDTO.getAccessToken());
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }


}