package com.deskflow.config;

import com.deskflow.ticket.Ticket;
import com.deskflow.ticket.TicketCategory;
import com.deskflow.ticket.TicketPriority;
import com.deskflow.ticket.TicketRepository;
import com.deskflow.ticket.TicketStatus;
import com.deskflow.user.Role;
import com.deskflow.user.User;
import com.deskflow.user.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Demo accounts for local runs. Turned off unless deskflow.seed-demo-data is true. */
@Configuration
@ConditionalOnProperty(name = "deskflow.seed-demo-data", havingValue = "true")
public class DemoDataSeeder {

    private static final String DEMO_PASSWORD = "Demo1234!";

    @Bean
    ApplicationRunner seedDemoData(UserRepository users, TicketRepository tickets, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            String hash = encoder.encode(DEMO_PASSWORD);
            User customer = users.save(new User("customer@deskflow.test", "Sara Customer", hash, Role.CUSTOMER));
            User agent = users.save(new User("agent@deskflow.test", "Adam Agent", hash, Role.AGENT));
            users.save(new User("admin@deskflow.test", "Nora Admin", hash, Role.ADMIN));

            tickets.save(new Ticket("Charged twice for my subscription",
                    "My card was charged twice this month. Please refund the second payment.",
                    customer, TicketPriority.HIGH, TicketCategory.BILLING, "Classified by keyword rules", false));

            tickets.save(new Ticket("How do I export my data?",
                    "I have a question about exporting my tickets to a spreadsheet.",
                    customer, TicketPriority.LOW, TicketCategory.GENERAL, "Classified by keyword rules", false));

            Ticket inProgress = new Ticket("Dashboard crashes on login",
                    "The dashboard shows an error and crashes right after I log in.",
                    customer, TicketPriority.HIGH, TicketCategory.TECHNICAL, "Classified by keyword rules", false);
            inProgress.moveTo(TicketStatus.IN_PROGRESS);
            inProgress.assignTo(agent);
            tickets.save(inProgress);
        };
    }
}
