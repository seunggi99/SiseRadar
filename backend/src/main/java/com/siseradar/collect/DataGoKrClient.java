package com.siseradar.collect;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.siseradar.collect.dto.RtmsApiResponse;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the data.go.kr RTMS endpoints. The operation path (e.g.
 * {@code RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade}) is passed in so one client serves every type.
 *
 * <p>Gotchas baked in: a {@code User-Agent} is mandatory (gateway blocks UA-less requests), and the
 * encoding service key is already percent-encoded so the URL is assembled by hand.
 */
@Component
public class DataGoKrClient {

  private static final Logger log = LoggerFactory.getLogger(DataGoKrClient.class);

  private final RestClient restClient;
  private final DataGoKrProperties props;

  // Standalone XmlMapper — NOT a Spring bean (XmlMapper extends ObjectMapper and would hijack
  // JSON response serialization via @ConditionalOnMissingBean(ObjectMapper)).
  private final XmlMapper xmlMapper =
      (XmlMapper) new XmlMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public DataGoKrClient(RestClient dataGoKrRestClient, DataGoKrProperties props) {
    this.restClient = dataGoKrRestClient;
    this.props = props;
  }

  public RtmsApiResponse fetch(
      String operationPath, String lawdCd, String dealYmd, int pageNo, int numOfRows) {
    String url =
        props.baseUrl()
            + "/" + operationPath
            + "?serviceKey=" + props.serviceKey()
            + "&LAWD_CD=" + lawdCd
            + "&DEAL_YMD=" + dealYmd
            + "&pageNo=" + pageNo
            + "&numOfRows=" + numOfRows;

    String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);

    try {
      RtmsApiResponse response = xmlMapper.readValue(body, RtmsApiResponse.class);
      String code = response.header != null ? response.header.resultCode : null;
      if (code != null && !"000".equals(code)) {
        throw new IllegalStateException(
            "data.go.kr error: %s %s"
                .formatted(code, response.header != null ? response.header.resultMsg : ""));
      }
      return response;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      String snippet = body == null ? "null" : body.substring(0, Math.min(body.length(), 300));
      log.error("Failed to parse data.go.kr response. First 300 chars: {}", snippet);
      throw new IllegalStateException("Failed to parse data.go.kr response", e);
    }
  }
}
