package com.mastercard.developer.transactionapi.client.impl;

import com.mastercard.developer.transactionapi.client.model.BatchResponse;
import com.mastercard.developer.transactionapi.client.model.ResponseItem;
import com.mastercard.developer.transactionapi.exception.TransactionApiException;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.ApiException;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.TransactionApiApi;
import org.openapitools.client.model.AuthorisationResponseV02Item;
import org.openapitools.client.model.AuthorisationResponseV02List;
import org.openapitools.client.model.AuthorisationinitiationAuthorisationInitiationV02;
import org.openapitools.client.model.AuthorisationresponseAuthorisationResponseV02;
import org.openapitools.client.model.FinancialAdviceResponseV02Item;
import org.openapitools.client.model.FinancialAdviceResponseV02List;
import org.openapitools.client.model.FinancialinitiationFinancialInitiationV02;
import org.openapitools.client.model.FinancialresponseFinancialResponseV02;
import org.openapitools.client.model.InquiryResponseV01Item;
import org.openapitools.client.model.InquiryResponseV01List;
import org.openapitools.client.model.InquiryinitiationInquiryInitiationV01;
import org.openapitools.client.model.InquiryresponseInquiryResponseV01;
import org.openapitools.client.model.ReversalResponseV02Item;
import org.openapitools.client.model.ReversalResponseV02List;
import org.openapitools.client.model.ReversalinitiationReversalInitiationV02;
import org.openapitools.client.model.ReversalresponseReversalResponseV02;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mastercard.developer.transactionapi.test.TestConstants.CORRELATION_ID_HEADER;
import static com.mastercard.developer.transactionapi.test.TestConstants.HTTP_STATUS_TOO_EARLY;
import static com.mastercard.developer.transactionapi.test.TestConstants.RETRY_AFTER_MS_HEADER;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_API_EXCEPTION_MESSAGE_AUTH;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_API_EXCEPTION_MESSAGE_FIN;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_API_EXCEPTION_MESSAGE_INQ;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_API_EXCEPTION_MESSAGE_REV;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_BATCH_LIMIT;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_CORRELATION_ID;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_RETRY_AFTER;
import static com.mastercard.developer.transactionapi.test.TestConstants.TEST_RETRY_AFTER_MS;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getAuthorisationResponseList;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getFinancialAdvResponseList;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getInquiryResponseList;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getReversalResponseList;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getTestAuthorisationInitiationV02;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getTestFinancialInitiationV02;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getTestInquiryInitiationV01;
import static com.mastercard.developer.transactionapi.test.TestRequestResponseGenerator.getTestReversalInitiationV02;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(MockitoExtension.class)
class TransactionApiClientImplTest {

    @InjectMocks
    private TransactionApiClientImpl transactionApiClient;
    @Mock
    private TransactionApiApi transactionApiApi;

    private final Map<String, List<String>> testPostResponseHeaders = new HashMap<String, List<String>>() {{
        put(CORRELATION_ID_HEADER, Collections.singletonList(TEST_CORRELATION_ID));
    }};

    private final Map<String, List<String>> testGetResponseHeaders = new HashMap<String, List<String>>() {{
        put(CORRELATION_ID_HEADER, Collections.singletonList(TEST_CORRELATION_ID));
        put(RETRY_AFTER_MS_HEADER, Collections.singletonList(Long.toString(TEST_RETRY_AFTER_MS)));
    }};

    private final Map<String, List<String>> testGetPartialResponseHeaders = new HashMap<String, List<String>>() {{
        put(CORRELATION_ID_HEADER, Collections.singletonList(TEST_CORRELATION_ID));
    }};

    private final ApiResponse<Void> testPostApiResponse = new ApiResponse<>(HttpStatus.SC_ACCEPTED, testPostResponseHeaders);

    private final ApiException testApiException = new ApiException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Failed");

    private final AuthorisationinitiationAuthorisationInitiationV02 testAuthInitiation = getTestAuthorisationInitiationV02();
    private final ReversalinitiationReversalInitiationV02 testRevInitiation = getTestReversalInitiationV02();
    private final InquiryinitiationInquiryInitiationV01 testInqInitiation = getTestInquiryInitiationV01();
    private final FinancialinitiationFinancialInitiationV02 testFinAdvInitiation = getTestFinancialInitiationV02();

    private final AuthorisationResponseV02List authList = getAuthorisationResponseList(HttpStatus.SC_OK);
    private final FinancialAdviceResponseV02List financialAdvList = getFinancialAdvResponseList(HttpStatus.SC_OK);
    private final InquiryResponseV01List inquiryList = getInquiryResponseList(HttpStatus.SC_OK);
    private final ReversalResponseV02List reversalList = getReversalResponseList(HttpStatus.SC_OK);

    @Test
    void givenHappyPath_whenSubmitAuthorisationRequest_thenReturnCorrId() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessAuthorisationRequestWithHttpInfo(testAuthInitiation))
                .thenReturn(testPostApiResponse);

        // call
        String resultCorrId = transactionApiClient.submitAuthorisationRequest(testAuthInitiation);

        // verify
        assertThat(resultCorrId).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void givenException_whenSubmitAuthorisationRequest_thenThrowException() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessAuthorisationRequestWithHttpInfo(testAuthInitiation))
                .thenThrow(testApiException);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.submitAuthorisationRequest(testAuthInitiation));

        // verify
        assertThat(e.getMessage()).isEqualTo(TEST_API_EXCEPTION_MESSAGE_AUTH);
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenHappyPath_whenSubmitReversalRequest_thenReturnCorrId() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessReversalRequestWithHttpInfo(testRevInitiation))
                .thenReturn(testPostApiResponse);

        // call
        String resultCorrId = transactionApiClient.submitReversalRequest(testRevInitiation);

        // verify
        assertThat(resultCorrId).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void givenException_whenSubmitReversalRequest_thenThrowException() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessReversalRequestWithHttpInfo(testRevInitiation))
                .thenThrow(testApiException);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.submitReversalRequest(testRevInitiation));

        // verify
        assertThat(e.getMessage()).isEqualTo(TEST_API_EXCEPTION_MESSAGE_REV);
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenHappyPath_whenSubmitInquiryRequest_thenReturnCorrId() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessInquiryRequestWithHttpInfo(testInqInitiation))
                .thenReturn(testPostApiResponse);

        // call
        String resultCorrId = transactionApiClient.submitInquiryRequest(testInqInitiation);

        // verify
        assertThat(resultCorrId).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void givenException_whenSubmitInquiryRequest_thenThrowException() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessInquiryRequestWithHttpInfo(testInqInitiation))
                .thenThrow(testApiException);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.submitInquiryRequest(testInqInitiation));

        // verify
        assertThat(e.getMessage()).isEqualTo(TEST_API_EXCEPTION_MESSAGE_INQ);
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenHappyPath_whenSubmitFinancialAdviceRequest_thenReturnCorrId() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessFinancialAdviceRequestWithHttpInfo(testFinAdvInitiation))
                .thenReturn(testPostApiResponse);

        // call
        String resultCorrId = transactionApiClient.submitFinancialAdviceRequest(testFinAdvInitiation);

        // verify
        assertThat(resultCorrId).isEqualTo(TEST_CORRELATION_ID);
    }

    @Test
    void givenException_whenSubmitFinancialAdviceRequest_thenThrowException() throws ApiException {
        // setup
        when(transactionApiApi.transactionApiProcessFinancialAdviceRequestWithHttpInfo(testFinAdvInitiation))
                .thenThrow(testApiException);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.submitFinancialAdviceRequest(testFinAdvInitiation));

        // verify
        assertThat(e.getMessage()).isEqualTo(TEST_API_EXCEPTION_MESSAGE_FIN);
        assertThat(e.getCause()).isSameAs(testApiException);
    }


    @Test
    void givenHappyPath_whenGetAuthorisationResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<AuthorisationResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_OK, testGetResponseHeaders, authList);
        when(transactionApiApi.transactionApiGetAuthorisationResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        AuthorisationResponseV02Item expectedItem1 = authList.getItems().get(0);
        AuthorisationResponseV02Item expectedItem2 = authList.getItems().get(1);

        // call
        BatchResponse<AuthorisationresponseAuthorisationResponseV02> responseActual = transactionApiClient.getAuthorisationResponses();

        // verify
        ResponseItem<AuthorisationresponseAuthorisationResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<AuthorisationresponseAuthorisationResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isFalse();
        assertThat(responseActual.getRetryAfter()).isEqualTo(TEST_RETRY_AFTER);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getAuthorisationResponses, httpStatus=200, itemsCount=2");
    }

    @Test
    void givenPartialContent_whenGetAuthorisationResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<AuthorisationResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_PARTIAL_CONTENT, testGetPartialResponseHeaders, authList);
        when(transactionApiApi.transactionApiGetAuthorisationResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        AuthorisationResponseV02Item expectedItem1 = authList.getItems().get(0);
        AuthorisationResponseV02Item expectedItem2 = authList.getItems().get(1);

        // call
        BatchResponse<AuthorisationresponseAuthorisationResponseV02> responseActual = transactionApiClient.getAuthorisationResponses();

        // verify
        ResponseItem<AuthorisationresponseAuthorisationResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<AuthorisationresponseAuthorisationResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isTrue();
        assertThat(responseActual.getRetryAfter()).isEqualTo(Duration.ZERO);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getAuthorisationResponses, httpStatus=206, itemsCount=2");
    }

    @Test
    void givenHappyPath_whenGetFinancialAdvResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<FinancialAdviceResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_OK, testGetResponseHeaders, financialAdvList);
        when(transactionApiApi.transactionApiGetFinancialAdviceResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        FinancialAdviceResponseV02Item expectedItem1 = financialAdvList.getItems().get(0);
        FinancialAdviceResponseV02Item expectedItem2 = financialAdvList.getItems().get(1);

        // call
        BatchResponse<FinancialresponseFinancialResponseV02> responseActual = transactionApiClient.getFinancialAdviceResponses();

        // verify
        ResponseItem<FinancialresponseFinancialResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<FinancialresponseFinancialResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isFalse();
        assertThat(responseActual.getRetryAfter()).isEqualTo(TEST_RETRY_AFTER);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getFinancialAdviceResponses, httpStatus=200, itemsCount=2");
    }

    @Test
    void givenPartialContent_whenGetFinancialAdvResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<FinancialAdviceResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_PARTIAL_CONTENT, testGetPartialResponseHeaders, financialAdvList);
        when(transactionApiApi.transactionApiGetFinancialAdviceResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        FinancialAdviceResponseV02Item expectedItem1 = financialAdvList.getItems().get(0);
        FinancialAdviceResponseV02Item expectedItem2 = financialAdvList.getItems().get(1);

        // call
        BatchResponse<FinancialresponseFinancialResponseV02> responseActual = transactionApiClient.getFinancialAdviceResponses();

        // verify
        ResponseItem<FinancialresponseFinancialResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<FinancialresponseFinancialResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isTrue();
        assertThat(responseActual.getRetryAfter()).isEqualTo(Duration.ZERO);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getFinancialAdviceResponses, httpStatus=206, itemsCount=2");
    }

    @Test
    void givenHappyPath_whenGetInquiryResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<InquiryResponseV01List> response =
                new ApiResponse<>(HttpStatus.SC_OK, testGetResponseHeaders, inquiryList);
        when(transactionApiApi.transactionApiGetInquiryResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        InquiryResponseV01Item expectedItem1 = inquiryList.getItems().get(0);
        InquiryResponseV01Item expectedItem2 = inquiryList.getItems().get(1);

        // call
        BatchResponse<InquiryresponseInquiryResponseV01> responseActual = transactionApiClient.getInquiryResponses();

        // verify
        ResponseItem<InquiryresponseInquiryResponseV01> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<InquiryresponseInquiryResponseV01> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isFalse();
        assertThat(responseActual.getRetryAfter()).isEqualTo(TEST_RETRY_AFTER);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getInquiryResponses, httpStatus=200, itemsCount=2");
    }

    @Test
    void givenPartialContent_whenGetInquiryResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<InquiryResponseV01List> response =
                new ApiResponse<>(HttpStatus.SC_PARTIAL_CONTENT, testGetPartialResponseHeaders, inquiryList);
        when(transactionApiApi.transactionApiGetInquiryResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        InquiryResponseV01Item expectedItem1 = inquiryList.getItems().get(0);
        InquiryResponseV01Item expectedItem2 = inquiryList.getItems().get(1);

        // call
        BatchResponse<InquiryresponseInquiryResponseV01> responseActual = transactionApiClient.getInquiryResponses();

        // verify
        ResponseItem<InquiryresponseInquiryResponseV01> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<InquiryresponseInquiryResponseV01> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isTrue();
        assertThat(responseActual.getRetryAfter()).isEqualTo(Duration.ZERO);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getInquiryResponses, httpStatus=206, itemsCount=2");
    }

    @Test
    void givenHappyPath_whenGetReversalResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<ReversalResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_OK, testGetResponseHeaders, reversalList);
        when(transactionApiApi.transactionApiGetReversalResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        ReversalResponseV02Item expectedItem1 = reversalList.getItems().get(0);
        ReversalResponseV02Item expectedItem2 = reversalList.getItems().get(1);

        // call
        BatchResponse<ReversalresponseReversalResponseV02> responseActual = transactionApiClient.getReversalResponses();

        // verify
        ResponseItem<ReversalresponseReversalResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<ReversalresponseReversalResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isFalse();
        assertThat(responseActual.getRetryAfter()).isEqualTo(TEST_RETRY_AFTER);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getReversalResponses, httpStatus=200, itemsCount=2");
    }

    @Test
    void givenPartialContent_whenGetReversalResponses_thenReturnBatchResponse(CapturedOutput output) throws ApiException {
        // setup
        ApiResponse<ReversalResponseV02List> response =
                new ApiResponse<>(HttpStatus.SC_PARTIAL_CONTENT, testGetPartialResponseHeaders, reversalList);
        when(transactionApiApi.transactionApiGetReversalResponsesWithHttpInfo(TEST_BATCH_LIMIT)).thenReturn(response);

        ReversalResponseV02Item expectedItem1 = reversalList.getItems().get(0);
        ReversalResponseV02Item expectedItem2 = reversalList.getItems().get(1);

        // call
        BatchResponse<ReversalresponseReversalResponseV02> responseActual = transactionApiClient.getReversalResponses();

        // verify
        ResponseItem<ReversalresponseReversalResponseV02> responseActualItem1 = responseActual.getItems().get(0);
        ResponseItem<ReversalresponseReversalResponseV02> responseActualItem2 = responseActual.getItems().get(1);

        assertThat(responseActual.isHasMore()).isTrue();
        assertThat(responseActual.getRetryAfter()).isEqualTo(Duration.ZERO);

        assertThat(responseActualItem1.getCorrelationId()).isEqualTo(expectedItem1.getCorrelationId());
        assertThat(responseActualItem2.getCorrelationId()).isEqualTo(expectedItem2.getCorrelationId());

        assertThat(responseActualItem1.getHttpStatus()).isEqualTo(expectedItem1.getHttpStatus());
        assertThat(responseActualItem2.getHttpStatus()).isEqualTo(expectedItem2.getHttpStatus());

        assertThat(responseActualItem1.getPayload()).isEqualTo(expectedItem1.getPayload());
        assertThat(responseActualItem2.getPayload()).isEqualTo(expectedItem2.getPayload());

        assertThat(responseActualItem1.getErrors()).isEqualTo(expectedItem1.getErrors());
        assertThat(responseActualItem2.getErrors()).isEqualTo(expectedItem2.getErrors());

        assertThat(output.getAll()).contains("Completed Transaction API getReversalResponses, httpStatus=206, itemsCount=2");
    }

    @Test
    void givenTooEarlyStatusCode_whenGetReversalResponses_thenVerifyTooEarlyResponse(CapturedOutput output) throws ApiException {
        // setup
        when(transactionApiApi.transactionApiGetReversalResponsesWithHttpInfo(TEST_BATCH_LIMIT))
                .thenThrow(new ApiException(HTTP_STATUS_TOO_EARLY, testGetResponseHeaders, null));

        // call
        BatchResponse<ReversalresponseReversalResponseV02> responseActual = transactionApiClient.getReversalResponses();

        // verify
        assertThat(responseActual.getItems()).isEqualTo(Collections.EMPTY_LIST);
        assertThat(responseActual.isHasMore()).isFalse();
        assertThat(responseActual.getRetryAfter()).isEqualTo(TEST_RETRY_AFTER);

        assertThat(output.getAll()).contains("Completed Transaction API getReversalResponses, httpStatus=425, itemsCount=0");
    }

    @Test
    void givenException_whenGetAuthorisationResponses_thenThrowTransactionApiException() throws ApiException {
        // setup
        doThrow(testApiException).when(transactionApiApi).transactionApiGetAuthorisationResponsesWithHttpInfo(TEST_BATCH_LIMIT);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.getAuthorisationResponses());

        // verify
        assertThat(e.getMessage()).contains("Failed to call getAuthorisationResponses");
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenException_whenGetFinancialAdvResponses_thenThrowTransactionApiException() throws ApiException {
        // setup
        doThrow(testApiException).when(transactionApiApi).transactionApiGetFinancialAdviceResponsesWithHttpInfo(TEST_BATCH_LIMIT);

        // call
        Exception e = assertThrows(TransactionApiException.class, () -> transactionApiClient.getFinancialAdviceResponses());

        // verify
        assertThat(e.getMessage()).contains("Failed to call getFinancialAdviceResponses");
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenException_whenGetInquiryResponses_thenThrowTransactionApiException() throws ApiException {
        // setup
        doThrow(testApiException).when(transactionApiApi).transactionApiGetInquiryResponsesWithHttpInfo(TEST_BATCH_LIMIT);

        // call
        Exception e = assertThrows(TransactionApiException.class, () -> transactionApiClient.getInquiryResponses());

        // verify
        assertThat(e.getMessage()).contains("Failed to call getInquiryResponses");
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenException_whenGetReversalResponses_thenThrowTransactionApiException() throws ApiException {
        // setup
        doThrow(testApiException).when(transactionApiApi).transactionApiGetReversalResponsesWithHttpInfo(TEST_BATCH_LIMIT);

        // call
        Exception e = assertThrows(TransactionApiException.class, () -> transactionApiClient.getReversalResponses());

        // verify
        assertThat(e.getMessage()).contains("Failed to call getReversalResponses");
        assertThat(e.getCause()).isSameAs(testApiException);
    }

    @Test
    void givenMissingCorrelationIdHeader_whenSubmitAuthorisationRequest_thenVerifyOutput() throws ApiException {
        // setup
        ApiResponse<Void> apiResponseWithNoCorrId = new ApiResponse<>(HttpStatus.SC_OK, Collections.emptyMap());
        when(transactionApiApi.transactionApiProcessAuthorisationRequestWithHttpInfo(testAuthInitiation))
                .thenReturn(apiResponseWithNoCorrId);

        // call
        Exception e = assertThrows(TransactionApiException.class, () ->
                transactionApiClient.submitAuthorisationRequest(testAuthInitiation));

        // verify
        assertThat(e.getMessage()).contains("Failed to call processAuthorisationRequest");
        assertThat(e.getCause().getMessage()).contains("Missing required Correlation-Id in the response");
    }

}