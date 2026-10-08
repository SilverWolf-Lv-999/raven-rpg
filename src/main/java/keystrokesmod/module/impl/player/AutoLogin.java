package keystrokesmod.module.impl.player;

import com.google.gson.JsonObject;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.module.setting.impl.TextSetting;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoLogin extends Module {
    private static final Pattern LOGIN_COMMAND_PATTERN = Pattern.compile("(?<![A-Za-z0-9_-])/(?:login|l)(?![A-Za-z0-9_-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTER_COMMAND_PATTERN = Pattern.compile("(?<![A-Za-z0-9_-])/(?:register|reg)(?![A-Za-z0-9_-])", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTER_USAGE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_-])/(?:register|reg)(?![A-Za-z0-9_-])\\s+(\\S+)(?:\\s+(\\S+))?", Pattern.CASE_INSENSITIVE);

    private final TextSetting password = new TextSetting("Password", "", "Type the server password...", 64) {
        @Override
        public void loadProfile(JsonObject data) {
            if (data == null || !data.has(getProfileKey()) || !data.get(getProfileKey()).isJsonPrimitive()) {
                return;
            }

            setText(data.getAsJsonPrimitive(getProfileKey()).getAsString());
        }
    };
    private final SliderSetting registerMode = new SliderSetting("Register mode", 0, new String[]{"Auto", "Single password", "Double password"});

    private String pendingCommand;
    private boolean pendingRegister;
    private boolean lastRegister;
    private long lastSentAt;

    public AutoLogin() {
        super("Auto Login", category.player);
        this.registerSetting(this.password);
        this.registerSetting(this.registerMode);
    }

    @Override
    public void onEnable() {
        this.clearPending();
    }

    @Override
    public void onDisable() {
        this.clearPending();
    }

    @Override
    public void onUpdate() {
        if (this.pendingCommand == null) {
            return;
        }

        String command = this.pendingCommand;
        boolean register = this.pendingRegister;
        this.pendingCommand = null;
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        mc.thePlayer.sendChatMessage(command);
        this.lastRegister = register;
        this.lastSentAt = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.message == null) {
            return;
        }

        String message = EnumChatFormatting.getTextWithoutFormattingCodes(e.message.getUnformattedText());
        String psd = this.password.getText().trim();
        if (message == null || message.isEmpty() || psd.isEmpty()) {
            return;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        Matcher loginMatcher = LOGIN_COMMAND_PATTERN.matcher(message);
        Matcher registerMatcher = REGISTER_COMMAND_PATTERN.matcher(message);
        boolean hasLogin = loginMatcher.find();
        boolean hasRegister = registerMatcher.find();
        int loginIndex = hasLogin ? loginMatcher.start() : Integer.MAX_VALUE;
        int registerIndex = hasRegister ? registerMatcher.start() : Integer.MAX_VALUE;
        String login = hasLogin ? loginMatcher.group().toLowerCase(Locale.ROOT) : null;
        String register = hasRegister ? registerMatcher.group().toLowerCase(Locale.ROOT) : null;
        hasLogin = hasLogin && this.isUsable(login, normalized);
        hasRegister = hasRegister && this.isUsable(register, normalized);
        if (!hasLogin && !hasRegister) {
            return;
        }

        if (hasRegister && (!hasLogin || registerIndex < loginIndex)) {
            this.queue(register + " " + psd + (this.useDoublePassword(message) ? " " + psd : ""), true);
            return;
        }

        this.queue(login + " " + psd, false);
    }

    private void queue(String command, boolean register) {
        if (this.pendingCommand != null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (this.lastRegister == register && now - this.lastSentAt < 2000L) {
            return;
        }

        this.pendingRegister = register;
        this.pendingCommand = command;
    }

    private boolean isUsable(String command, String normalizedMessage) {
        if (!command.equals("/l") && !command.equals("/reg")) {
            return true;
        }

        return normalizedMessage.contains("password")
                || normalizedMessage.contains("pass")
                || normalizedMessage.contains("login")
                || normalizedMessage.contains("log in")
                || normalizedMessage.contains("register")
                || normalizedMessage.contains("auth")
                || normalizedMessage.contains("密码")
                || normalizedMessage.contains("登录")
                || normalizedMessage.contains("登陆")
                || normalizedMessage.contains("登入")
                || normalizedMessage.contains("注册")
                || normalizedMessage.contains("认证");
    }

    private boolean useDoublePassword(String message) {
        int mode = (int) this.registerMode.getInput();
        if (mode == 1) {
            return false;
        }
        if (mode == 2) {
            return true;
        }

        Matcher matcher = REGISTER_USAGE_PATTERN.matcher(message);
        while (matcher.find()) {
            String firstArgument = cleanUsageToken(matcher.group(1));
            String secondArgument = cleanUsageToken(matcher.group(2));
            if (!secondArgument.isEmpty() && !isUsageSeparator(secondArgument)) {
                return true;
            }
            if (!firstArgument.isEmpty() && !isUsageSeparator(firstArgument)) {
                return false;
            }
        }

        return true;
    }

    private static String cleanUsageToken(String token) {
        if (token == null) {
            return "";
        }

        int start = 0;
        int end = token.length();
        while (start < end && isUsagePunctuation(token.charAt(start))) {
            start++;
        }
        while (end > start && isUsagePunctuation(token.charAt(end - 1))) {
            end--;
        }

        return token.substring(start, end).toLowerCase(Locale.ROOT);
    }

    private static boolean isUsagePunctuation(char character) {
        return character == '<'
                || character == '>'
                || character == '['
                || character == ']'
                || character == '('
                || character == ')'
                || character == '{'
                || character == '}'
                || character == '"'
                || character == '\''
                || character == '`'
                || character == ','
                || character == '.'
                || character == ':'
                || character == ';'
                || character == '|'
                || character == '，'
                || character == '。'
                || character == '：'
                || character == '；'
                || character == '！'
                || character == '!'
                || character == '?'
                || character == '？'
                || character == '“'
                || character == '”'
                || character == '‘'
                || character == '’';
    }

    private static boolean isUsageSeparator(String token) {
        return token.isEmpty()
                || token.startsWith("/")
                || token.equals("or")
                || token.equals("and")
                || token.equals("或")
                || token.equals("或者")
                || token.equals("和");
    }

    private void clearPending() {
        this.pendingCommand = null;
        this.pendingRegister = false;
        this.lastRegister = false;
        this.lastSentAt = 0L;
    }
}
