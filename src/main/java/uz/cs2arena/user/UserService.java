package uz.cs2arena.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.cs2arena.auth.SteamClient;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.config.AppProperties;
import uz.cs2arena.wallet.WalletService;

@Service
public class UserService {

    private final UserRepository users;
    private final SteamClient steam;
    private final WalletService wallets;
    private final AppProperties props;

    public UserService(UserRepository users, SteamClient steam, WalletService wallets, AppProperties props) {
        this.users = users;
        this.steam = steam;
        this.wallets = wallets;
        this.props = props;
    }

    /** Steam orqali kirish: yangi bo'lsa yaratadi, bor bo'lsa avatarini yangilaydi. */
    @Transactional
    public User loginWithSteam(String steamId) {
        var profile = steam.fetchProfile(steamId);
        User user = users.findBySteamId(steamId).orElseGet(() -> {
            String nick = profile.map(SteamClient.SteamProfile::nickname)
                    .orElse("Player_" + steamId.substring(steamId.length() - 4));
            User created = users.save(new User(steamId, truncate(nick, 32)));
            wallets.createFor(created);
            return created;
        });
        profile.map(SteamClient.SteamProfile::avatarUrl).ifPresent(user::setAvatarUrl);
        if (props.superAdminSteamIds().contains(steamId) && user.getRole() != Role.SUPER_ADMIN) {
            user.setRole(Role.SUPER_ADMIN);
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("Foydalanuvchi topilmadi"));
    }

    @Transactional
    public User updateSettings(Long userId, UserDtos.UpdateSettingsRequest req) {
        User u = get(userId);
        if (req.nickname() != null) u.setNickname(req.nickname().trim());
        if (req.country() != null) u.setCountry(req.country().trim());
        if (req.city() != null) u.setCity(req.city().trim());
        if (req.language() != null) u.setLanguage(req.language());
        if (req.notifyMatchSound() != null) u.setNotifyMatchSound(req.notifyMatchSound());
        if (req.notifyFriendRequests() != null) u.setNotifyFriendRequests(req.notifyFriendRequests());
        if (req.notifyTournaments() != null) u.setNotifyTournaments(req.notifyTournaments());
        if (req.notifyEmail() != null) u.setNotifyEmail(req.notifyEmail());
        return u;
    }

    /** Steam'dan nik va avatarni qayta olish. */
    @Transactional
    public User syncFromSteam(Long userId) {
        User u = get(userId);
        var profile = steam.fetchProfile(u.getSteamId())
                .orElseThrow(() -> ApiException.badRequest("STEAM_UNAVAILABLE",
                        "Steam profilini olib bo'lmadi (API kalit sozlanmagan bo'lishi mumkin)"));
        u.setNickname(truncate(profile.nickname(), 32));
        u.setAvatarUrl(profile.avatarUrl());
        return u;
    }

    @Transactional(readOnly = true)
    public long rankOf(User u) {
        return u.getMatchesPlayed() == 0 ? 0 : users.countAboveElo(u.getElo()) + 1;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
