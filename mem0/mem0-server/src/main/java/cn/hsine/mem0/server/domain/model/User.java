package cn.hsine.mem0.server.domain.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a user account for authentication.
 *
 * @author MoBai
 */
@Data
@EqualsAndHashCode
@ToString
public class User {

    private String id;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Password hash cannot be blank")
    private String passwordHash;

    private String role;

    @NotNull
    private Date createdAt;

    private Date lastLoginAt;

    public User() {
    }

    /**
     * Creates a new user.
     *
     * @param email        the email address
     * @param passwordHash the hashed password
     * @param name         the user's name
     */
    public User(String email, String passwordHash, String name) {
        this();
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }

}
