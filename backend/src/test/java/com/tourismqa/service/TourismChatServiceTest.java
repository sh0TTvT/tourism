package com.tourismqa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourismqa.dto.ChatRequest;
import com.tourismqa.dto.ChatResponse;
import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.entity.ChatConversation;
import com.tourismqa.entity.ChatMessage;
import com.tourismqa.entity.UserAccount;
import com.tourismqa.entity.UserRole;
import com.tourismqa.repository.ChatConversationRepository;
import com.tourismqa.repository.ChatMessageRepository;
import com.tourismqa.repository.RoutePlanRepository;
import com.tourismqa.repository.RoutePointRepository;
import com.tourismqa.repository.UserAccountRepository;
import com.tourismqa.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class TourismChatServiceTest {

    @Mock
    private LlmRouterService llmRouterService;

    @Mock
    private ModelCatalogService modelCatalogService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RoutePlanRepository routePlanRepository;

    @Mock
    private RoutePointRepository routePointRepository;

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    @Mock
    private RealtimeTravelContextService realtimeTravelContextService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private TourismChatService tourismChatService;

    @BeforeEach
    void setUp() {
        tourismChatService = new TourismChatService(
                llmRouterService,
                modelCatalogService,
                userAccountRepository,
                chatConversationRepository,
                chatMessageRepository,
                routePlanRepository,
                routePointRepository,
                knowledgeGraphService,
                realtimeTravelContextService,
                new ObjectMapper(),
                transactionManager
        );
    }

    @Test
    void chat_bypassesLlmForDirectWeatherAnswer() {
        UserAccount user = user(1L);
        UserPrincipal principal = new UserPrincipal(user);
        ChatConversation conversation = conversation(10L, user);
        KgContextResponse graphContext = new KgContextResponse(null, List.of(), List.of(), List.of());

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelCatalogService.resolveModelSelection(null, null))
                .thenReturn(new LlmModelSelection("siliconflow", "Qwen/Qwen2.5-7B-Instruct"));
        when(chatConversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(knowledgeGraphService.buildLlmContext("北京明天天气")).thenReturn(graphContext);
        when(realtimeTravelContextService.tryBuildDirectAnswer("北京明天天气", graphContext, null))
                .thenReturn(new RealtimeTravelContextService.DirectRealtimeAnswer("真实天气结果", true, false));
        when(chatConversationRepository.findById(10L)).thenReturn(Optional.of(conversation));

        ChatResponse response = tourismChatService.chat(
                new ChatRequest("北京明天天气", null, null, null, null, null),
                principal
        );

        assertEquals("真实天气结果", response.answer());
        assertTrue(response.usedWeatherContext());
        assertFalse(response.usedAttractionStatusContext());
        verify(llmRouterService, never()).chatResolved(anyString(), anyString(), anyList(), anyDouble());
    }

    @Test
    void streamChat_persistsCompleteAnswerAfterClientDisconnectsDuringDeltaWrite() throws Exception {
        UserAccount user = user(1L);
        UserPrincipal principal = new UserPrincipal(user);
        ChatConversation conversation = conversation(10L, user);
        KgContextResponse graphContext = new KgContextResponse(null, List.of(), List.of(), List.of());
        String firstDelta = "a".repeat(101);
        String secondDelta = "b";

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelCatalogService.resolveModelSelection(null, null))
                .thenReturn(new LlmModelSelection("siliconflow", "Qwen/Qwen2.5-7B-Instruct"));
        when(chatConversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(knowledgeGraphService.buildLlmContext("推荐北京两日游")).thenReturn(graphContext);
        when(realtimeTravelContextService.tryBuildDirectAnswer("推荐北京两日游", graphContext, null))
                .thenReturn(null);
        when(realtimeTravelContextService.buildPromptContext("推荐北京两日游", graphContext, null))
                .thenReturn(null);
        when(chatConversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        org.mockito.Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onDelta = invocation.getArgument(4);
            onDelta.accept(firstDelta);
            onDelta.accept(secondDelta);
            return null;
        }).when(llmRouterService).streamResolved(anyString(), anyString(), anyList(), anyDouble(), any());

        tourismChatService.streamChat(
                new ChatRequest("推荐北京两日游", null, null, null, null, null),
                principal
        ).writeTo(new DisconnectOnDeltaOutputStream());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());

        List<ChatMessage> savedMessages = messageCaptor.getAllValues();
        assertEquals("user", savedMessages.get(0).getRole());
        assertEquals("推荐北京两日游", savedMessages.get(0).getContent());
        assertEquals("assistant", savedMessages.get(1).getRole());
        assertEquals(firstDelta + secondDelta, savedMessages.get(1).getContent());
    }

    @Test
    void streamChat_persistsPartialAnswerAfterModelStreamFails() throws Exception {
        UserAccount user = user(1L);
        UserPrincipal principal = new UserPrincipal(user);
        ChatConversation conversation = conversation(10L, user);
        KgContextResponse graphContext = new KgContextResponse(null, List.of(), List.of(), List.of());
        String partialAnswer = "部分回答";

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelCatalogService.resolveModelSelection(null, null))
                .thenReturn(new LlmModelSelection("siliconflow", "Qwen/Qwen2.5-7B-Instruct"));
        when(chatConversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(knowledgeGraphService.buildLlmContext("推荐北京两日游")).thenReturn(graphContext);
        when(realtimeTravelContextService.tryBuildDirectAnswer("推荐北京两日游", graphContext, null))
                .thenReturn(null);
        when(realtimeTravelContextService.buildPromptContext("推荐北京两日游", graphContext, null))
                .thenReturn(null);
        when(chatConversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        org.mockito.Mockito.doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onDelta = invocation.getArgument(4);
            onDelta.accept(partialAnswer);
            throw new IOException("model stream disconnected");
        }).when(llmRouterService).streamResolved(anyString(), anyString(), anyList(), anyDouble(), any());

        tourismChatService.streamChat(
                new ChatRequest("推荐北京两日游", null, null, null, null, null),
                principal
        ).writeTo(OutputStream.nullOutputStream());

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).save(messageCaptor.capture());

        List<ChatMessage> savedMessages = messageCaptor.getAllValues();
        assertEquals("user", savedMessages.get(0).getRole());
        assertEquals("推荐北京两日游", savedMessages.get(0).getContent());
        assertEquals("assistant", savedMessages.get(1).getRole());
        assertEquals(partialAnswer, savedMessages.get(1).getContent());
    }

    private UserAccount user(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setDisplayName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRole.USER);
        return user;
    }

    private ChatConversation conversation(Long id, UserAccount user) {
        ChatConversation conversation = new ChatConversation();
        conversation.setId(id);
        conversation.setUser(user);
        conversation.setTitle("weather");
        conversation.setProvider("siliconflow");
        conversation.setModel("Qwen/Qwen2.5-7B-Instruct");
        return conversation;
    }

    private static class DisconnectOnDeltaOutputStream extends OutputStream {
        private final List<String> lines = new ArrayList<>();
        private final StringBuilder currentLine = new StringBuilder();

        @Override
        public void write(int b) throws IOException {
            if (b == '\n') {
                String line = currentLine.toString();
                currentLine.setLength(0);
                lines.add(line);

                if (line.contains("\"type\":\"delta\"")) {
                    throw new IOException("client disconnected");
                }
                return;
            }

            currentLine.append((char) b);
        }
    }
}
