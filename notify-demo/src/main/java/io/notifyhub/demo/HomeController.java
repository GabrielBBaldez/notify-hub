package io.notifyhub.demo;

import io.notifyhub.core.NotifyHub;
import io.notifyhub.core.NotificationTracker;
import io.notifyhub.core.audience.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final NotifyHub notify;

    @Autowired(required = false)
    private EmbeddedSmtpConfig smtpConfig;

    @Autowired(required = false)
    private NotificationTracker tracker;

    @Autowired(required = false)
    private ContactRepository contactRepository;

    public HomeController(NotifyHub notify) {
        this.notify = notify;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("profile", smtpConfig != null ? "embedded SMTP" : "external SMTP");
        model.addAttribute("trackingEnabled", tracker != null);
        model.addAttribute("trackingCount", tracker != null ? tracker.count() : 0);
        model.addAttribute("audienceEnabled", contactRepository != null);

        List<String> channels = notify.getRegisteredChannels().stream()
                .sorted()
                .collect(Collectors.toList());
        model.addAttribute("channels", channels);
        model.addAttribute("channelCount", channels.size());

        return "home";
    }
}
