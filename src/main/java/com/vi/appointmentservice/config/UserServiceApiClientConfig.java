package com.vi.appointmentservice.config;

import com.vi.appointmentservice.userservice.generated.ApiClient;
import com.vi.appointmentservice.userservice.generated.web.UserControllerApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserServiceApiClientConfig {

  @Value("${user.service.api.url}")
  private String userServiceApiUrl;

  @Bean(name = "regularUser")
  public UserControllerApi userControllerApi(RestTemplate restTemplate) {
    ApiClient apiClient = new UserApiClient(restTemplate);
    apiClient.setBasePath(this.userServiceApiUrl);
    return new UserControllerApi(apiClient);
  }
}