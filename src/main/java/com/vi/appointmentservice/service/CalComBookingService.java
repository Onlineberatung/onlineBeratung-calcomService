package com.vi.appointmentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vi.appointmentservice.api.model.CalcomBooking;
import com.vi.appointmentservice.helper.RescheduleHelper;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CalComBookingService extends CalComService {

  private final @NonNull RescheduleHelper rescheduleHelper;
  private final @NonNull CalcomRepository calcomRepository;

  @Autowired
  public CalComBookingService(RestTemplate restTemplate,
      @Value("${calcom.apiUrl}") String calcomApiUrl,
      @Value("${calcom.apiKey}") String calcomApiKey,
      @NonNull RescheduleHelper rescheduleHelper, CalcomRepository calcomRepository) {
    super(restTemplate, calcomApiUrl, calcomApiKey);
    this.rescheduleHelper = rescheduleHelper;
    this.calcomRepository = calcomRepository;
  }

  // Booking
  public List<CalcomBooking> getAllBookings() throws JsonProcessingException {
    String response = this.restTemplate
        .getForObject(String.format(this.buildUri("/v1/bookings"), calcomApiUrl, calcomApiKey),
            String.class);
    JSONObject jsonObject = new JSONObject(response);
    response = jsonObject.getJSONArray("bookings").toString();
    ObjectMapper mapper = new ObjectMapper();
    List<CalcomBooking> result = List
        .of(Objects.requireNonNull(mapper.readValue(response, CalcomBooking[].class)));
    log.info("Found total of {} bookings", result.size());
    return result;
  }

  public List<CalcomBooking> getAllBookingsForConsultant(Long userId)
      throws JsonProcessingException {
    List<CalcomBooking> consultantBooking = calcomRepository.getAllBookingsByStatus(userId);
    for (CalcomBooking booking : consultantBooking) {
      rescheduleHelper.attachRescheduleLink(booking);
      rescheduleHelper.attachAskerName(booking);
    }
    return consultantBooking;
  }

  public CalcomBooking createBooking(CalcomBooking booking) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    JSONObject bookingObject = new JSONObject(booking);
    log.debug("Creating booking: {}", bookingObject);
    HttpEntity<String> request = new HttpEntity<>(bookingObject.toString(), headers);
    String askerIdParamPath = null;

    return restTemplate.postForEntity(this.buildUri("/v1/bookings"), request, CalcomBooking.class)
        .getBody();
  }

  public CalcomBooking updateBooking(CalcomBooking booking) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    JSONObject bookingObject = new JSONObject(booking);
    log.debug("Updating calcom user: {}", bookingObject);
    HttpEntity<String> request = new HttpEntity<>(bookingObject.toString(), headers);
    return restTemplate.postForEntity(this.buildUri("/v1/bookings/" + booking.getId()), request,
        CalcomBooking.class).getBody();
  }


  public CalcomBooking getBookingById(Long bookingId) {
    String response = restTemplate.getForObject(
        String.format(this.buildUri("/v1/bookings/" + bookingId), calcomApiUrl, calcomApiKey),
        String.class);
    JSONObject jsonObject = new JSONObject(response);
    log.debug(String.valueOf(jsonObject));
    response = jsonObject.getJSONObject("booking").toString();
    log.debug(response);
    ObjectMapper mapper = new ObjectMapper();
    try {
      CalcomBooking calcomBooking = mapper.readValue(response, CalcomBooking.class);
      calcomBooking.setStartTime(
          ZonedDateTime.parse(
              jsonObject.getJSONObject("booking").get("startTime").toString()).plusHours(2)
              .toString());
      calcomBooking.setEndTime(
          ZonedDateTime.parse(
              jsonObject.getJSONObject("booking").get("endTime").toString()).plusHours(2)
              .toString());
      return calcomBooking;
    } catch (JsonProcessingException e) {
      return null;
    }

  }
}
