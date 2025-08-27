// chatbot-embed.js - 임베드 가능한 챗봇 위젯
(function() {
    'use strict';

    class EmbeddableChatbot {
        constructor(config = {}) {
            // 전역 설정과 매개변수 설정을 병합
            const globalConfig = window.chatbotConfig || {};
            this.config = {
                apiUrl: config.apiUrl || globalConfig.apiUrl || this.getCurrentDomain(),
                position: config.position || globalConfig.position || 'bottom-right',
                theme: config.theme || globalConfig.theme || 'default',
                userId: config.userId || globalConfig.userId || 'external_user_' + Date.now(),
                ...config,
                ...globalConfig
            };
            this.isOpen = false;
            this.historyLoaded = false;
            this.init();
        }

        getCurrentDomain() {
            // 현재 스크립트가 로드된 도메인을 추출
            const scripts = document.getElementsByTagName('script');
            for (let script of scripts) {
                if (script.src && script.src.includes('chatbot-embed.js')) {
                    const url = new URL(script.src);
                    return `${url.protocol}//${url.host}`;
                }
            }
            // 기본값 (개발용)
            return 'http://localhost:8088';
        }

        init() {
            // 기존 위젯이 있는지 확인 (upload.html처럼 이미 위젯이 있는 페이지)
            if (document.getElementById('chatbotWidget')) {
                console.log('Chatbot widget already exists on this page');
                return;
            }
            
            this.loadStyles();
            this.createWidget();
            this.loadMarked();
        }

        loadStyles() {
            const styles = `
                /* 챗봇 플로팅 버튼 */
                .chatbot-toggle-embed {
                    position: fixed;
                    bottom: 20px;
                    right: 20px;
                    width: 60px;
                    height: 60px;
                    background: #d10f32;
                    border-radius: 50%;
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                    cursor: pointer;
                    z-index: 10000;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    transition: all 0.3s ease;
                    border: none;
                }

                .chatbot-toggle-embed:hover {
                    background: #d10f32;
                    transform: scale(1.1);
                    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
                }

                .chatbot-icon-embed {
                    width: 32px;
                    height: 32px;
                    background-image: url('https://static.elandrs.com/f/img/favicon/emall.ico');
                    background-size: contain;
                    background-repeat: no-repeat;
                    background-position: center;
                }

                /* 챗봇 위젯 컨테이너 */
                .chatbot-widget-embed {
                    position: fixed;
                    bottom: 90px;
                    right: 20px;
                    width: 380px;
                    height: 600px;
                    background: white;
                    border-radius: 15px;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
                    z-index: 9999;
                    display: none;
                    flex-direction: column;
                    overflow: hidden;
                    transform: scale(0);
                    transform-origin: bottom right;
                    transition: all 0.3s ease;
                }

                .chatbot-widget-embed.show {
                    display: flex;
                    transform: scale(1);
                }

                /* 챗봇 헤더 */
                .widget-header-embed {
                    background: #667eea;
                    color: white;
                    padding: 15px 20px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }

                .widget-title-embed {
                    font-weight: bold;
                    font-size: 16px;
                }

                .close-btn-embed {
                    background: none;
                    border: none;
                    color: white;
                    font-size: 20px;
                    cursor: pointer;
                    padding: 0;
                    width: 24px;
                    height: 24px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                }

                .close-btn-embed:hover {
                    background: rgba(255, 255, 255, 0.2);
                    border-radius: 50%;
                }

                /* 챗봇 메시지 영역 */
                .widget-messages-embed {
                    flex: 1;
                    padding: 15px 15px 15px 35px;
                    overflow-y: auto;
                    display: flex;
                    flex-direction: column;
                    gap: 10px;
                    background: #f8f9fa;
                }

                .widget-message-embed {
                    max-width: 85%;
                    padding: 10px 14px;
                    border-radius: 15px;
                    word-wrap: break-word;
                    line-height: 1.4;
                    font-size: 14px;
                }

                .widget-user-message-embed {
                    background: #667eea;
                    color: white;
                    align-self: flex-end;
                    margin-left: auto;
                }

                .widget-bot-message-embed {
                    background: white;
                    color: #333;
                    align-self: flex-start;
                    position: relative;
                    border: 1px solid #e9ecef;
                }

                .widget-bot-message-embed::before {
                    content: '';
                    position: absolute;
                    left: -28px;
                    top: 10px;
                    width: 18px;
                    height: 18px;
                    background-image: url('https://static.elandrs.com/f/img/favicon/emall.ico');
                    background-size: contain;
                    background-repeat: no-repeat;
                    background-position: center;
                }

                /* 챗봇 입력 영역 */
                .widget-input-embed {
                    padding: 15px;
                    border-top: 1px solid #e9ecef;
                    display: flex;
                    gap: 8px;
                    background: white;
                }

                .widget-input-embed input {
                    flex: 1;
                    padding: 10px 12px;
                    border: 1px solid #ddd;
                    border-radius: 20px;
                    outline: none;
                    font-size: 14px;
                }

                .widget-input-embed input:focus {
                    border-color: #667eea;
                }

                .widget-input-embed button {
                    background: #667eea;
                    color: white;
                    border: none;
                    padding: 10px 16px;
                    border-radius: 20px;
                    cursor: pointer;
                    font-size: 14px;
                }

                .widget-input-embed button:hover {
                    background: #5a67d8;
                }

                .widget-input-embed button:disabled {
                    background: #ccc;
                    cursor: not-allowed;
                }

                /* 타이핑 인디케이터 */
                .widget-typing-embed {
                    display: none;
                    align-self: flex-start;
                    padding: 10px 14px;
                    background: white;
                    border: 1px solid #e9ecef;
                    border-radius: 15px;
                    color: #666;
                    font-style: italic;
                    font-size: 14px;
                }

                .typing-dots-embed::after {
                    content: '';
                    animation: typing-embed 1.5s infinite;
                }

                @keyframes typing-embed {
                    0%, 33% { content: '.'; }
                    34%, 66% { content: '..'; }
                    67%, 100% { content: '...'; }
                }

                /* 마크다운 스타일링 (위젯용으로 축소) */
                .widget-bot-message-embed h1, .widget-bot-message-embed h2, .widget-bot-message-embed h3 {
                    margin: 4px 0 2px 0;
                    font-weight: bold;
                    font-size: 1.1em;
                }

                .widget-bot-message-embed p {
                    margin: 2px 0;
                }

                .widget-bot-message-embed ul, .widget-bot-message-embed ol {
                    margin: 4px 0;
                    padding-left: 16px;
                }

                .widget-bot-message-embed li {
                    margin: 1px 0;
                }

                .widget-bot-message-embed code {
                    background: #f1f3f4;
                    padding: 1px 3px;
                    border-radius: 2px;
                    font-family: monospace;
                    font-size: 0.9em;
                }

                .widget-bot-message-embed strong {
                    font-weight: bold;
                }

                /* 모바일 반응형 */
                @media (max-width: 480px) {
                    .chatbot-widget-embed {
                        width: calc(100vw - 40px);
                        height: calc(100vh - 140px);
                        bottom: 90px;
                        right: 20px;
                        left: 20px;
                    }
                }
            `;

            // 기존 스타일이 있는지 확인하고 없을 때만 추가
            if (!document.getElementById('chatbot-embed-styles')) {
                const styleSheet = document.createElement('style');
                styleSheet.id = 'chatbot-embed-styles';
                styleSheet.textContent = styles;
                document.head.appendChild(styleSheet);
            }
        }

        loadMarked() {
            if (!window.marked) {
                const script = document.createElement('script');
                script.src = 'https://cdn.jsdelivr.net/npm/marked/marked.min.js';
                script.onload = () => this.setupMarked();
                document.head.appendChild(script);
            } else {
                this.setupMarked();
            }
        }

        setupMarked() {
            if (window.marked && window.marked.setOptions) {
                marked.setOptions({
                    breaks: true,
                    gfm: true,
                    sanitize: false,
                    smartypants: true
                });
            }
        }

        createWidget() {
            const widgetHTML = `
                <!-- 챗봇 플로팅 버튼 -->
                <button class="chatbot-toggle-embed" id="chatbotToggleEmbed">
                    <div class="chatbot-icon-embed"></div>
                </button>

                <!-- 챗봇 위젯 -->
                <div class="chatbot-widget-embed" id="chatbotWidgetEmbed">
                    <div class="widget-header-embed">
                        <span class="widget-title-embed">AI 챗봇</span>
                        <button class="close-btn-embed" id="closeBtnEmbed">×</button>
                    </div>

                    <div class="widget-messages-embed" id="widgetMessagesEmbed">
                        <div class="widget-message-embed widget-bot-message-embed">
                            안녕하세요! 무엇을 도와드릴까요?
                        </div>
                    </div>

                    <div class="widget-typing-embed" id="widgetTypingEmbed">
                        <span class="typing-dots-embed">AI가 응답을 생성 중입니다</span>
                    </div>

                    <div class="widget-input-embed">
                        <input type="text" id="widgetInputEmbed" placeholder="메시지를 입력하세요..." />
                        <button id="widgetSendBtnEmbed">전송</button>
                    </div>
                </div>
            `;

            const widgetContainer = document.createElement('div');
            widgetContainer.id = 'chatbot-embed-container';
            widgetContainer.innerHTML = widgetHTML;
            document.body.appendChild(widgetContainer);

            // DOM 요소 참조 설정
            this.chatMessages = document.getElementById('widgetMessagesEmbed');
            this.messageInput = document.getElementById('widgetInputEmbed');
            this.sendButton = document.getElementById('widgetSendBtnEmbed');
            this.typingIndicator = document.getElementById('widgetTypingEmbed');
            this.widget = document.getElementById('chatbotWidgetEmbed');
            this.toggleBtn = document.getElementById('chatbotToggleEmbed');
            this.closeBtn = document.getElementById('closeBtnEmbed');

            // 이벤트 리스너 등록
            this.bindEvents();
        }

        bindEvents() {
            if (this.toggleBtn) this.toggleBtn.addEventListener('click', () => this.toggleWidget());
            if (this.closeBtn) this.closeBtn.addEventListener('click', () => this.closeWidget());
            if (this.sendButton) this.sendButton.addEventListener('click', () => this.sendMessage());
            if (this.messageInput) {
                this.messageInput.addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') this.sendMessage();
                });
            }
        }

        toggleWidget() {
            this.isOpen ? this.closeWidget() : this.openWidget();
        }

        openWidget() {
            if (this.widget) {
                this.widget.classList.add('show');
                this.isOpen = true;
                if (this.messageInput) this.messageInput.focus();
                if (!this.historyLoaded) {
                    this.loadChatHistory();
                }
            }
        }

        closeWidget() {
            if (this.widget) {
                this.widget.classList.remove('show');
                this.isOpen = false;
            }
        }

        formatMessage(text) {
            try {
                if (window.marked && window.marked.parse) {
                    const htmlContent = marked.parse(text);
                    return htmlContent.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '');
                }
            } catch (error) {
                console.error('Markdown parsing error:', error);
            }
            return text.replace(/\n/g, '<br>');
        }

        addMessage(message, isUser = false) {
            if (!this.chatMessages) return;

            const messageDiv = document.createElement('div');
            messageDiv.className = `widget-message-embed ${isUser ? 'widget-user-message-embed' : 'widget-bot-message-embed'}`;

            if (isUser) {
                messageDiv.textContent = message;
            } else {
                messageDiv.innerHTML = this.formatMessage(message);
            }

            this.chatMessages.appendChild(messageDiv);
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        }

        showTyping() {
            if (this.typingIndicator) {
                this.typingIndicator.style.display = 'block';
                this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
            }
        }

        hideTyping() {
            if (this.typingIndicator) {
                this.typingIndicator.style.display = 'none';
            }
        }

        async loadChatHistory() {
            try {
                // const userId = this.config.userId;
                const userId = 'lee_junyoung06'
                const response = await fetch(`${this.config.apiUrl}/chat/history/${userId}`, {
                    method: 'POST'
                });

                if (!response.ok) throw new Error('채팅 기록을 불러오는데 실패했습니다.');

                const history = await response.json();
                // 기본 메시지 제거
                if (this.chatMessages) {
                    this.chatMessages.innerHTML = '';
                    
                    if (history.length === 0) {
                        this.addMessage('안녕하세요! 무엇을 도와드릴까요?');
                    } else {
                        history.forEach(message => {
                            this.addMessage(message.content, message.type === 'USER');
                        });
                    }
                }
                this.historyLoaded = true;
            } catch (error) {
                console.error('채팅 기록 로딩 오류:', error);
                this.addMessage('안녕하세요! 무엇을 도와드릴까요?');
            }
        }

        async sendMessage() {
            if (!this.messageInput || !this.sendButton) return;
            
            const message = this.messageInput.value.trim();
            if (!message) return;

            this.addMessage(message, true);
            this.messageInput.value = '';
            this.sendButton.disabled = true;
            this.showTyping();

            try {
                const response = await fetch(`${this.config.apiUrl}/chat/stream`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text: message })
                });

                if (!response.ok) throw new Error('네트워크 응답이 올바르지 않습니다.');

                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let botMessage = '';

                this.hideTyping();
                const botMessageDiv = document.createElement('div');
                botMessageDiv.className = 'widget-message-embed widget-bot-message-embed';
                if (this.chatMessages) this.chatMessages.appendChild(botMessageDiv);

                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;

                    const chunk = decoder.decode(value);
                    botMessage += chunk;
                    botMessageDiv.innerHTML = this.formatMessage(botMessage);
                    if (this.chatMessages) this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
                }
            } catch (error) {
                this.hideTyping();
                this.addMessage('죄송합니다. 오류가 발생했습니다: ' + error.message);
            } finally {
                this.sendButton.disabled = false;
            }
        }
    }

    // 전역으로 사용할 수 있도록 설정
    window.EmbeddableChatbot = EmbeddableChatbot;

    // 자동 초기화 (페이지에 위젯이 없을 때만)
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            new EmbeddableChatbot();
        });
    } else {
        new EmbeddableChatbot();
    }
})();
