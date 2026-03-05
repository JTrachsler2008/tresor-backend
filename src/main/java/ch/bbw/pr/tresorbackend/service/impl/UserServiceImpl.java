package ch.bbw.pr.tresorbackend.service.impl;

import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.repository.UserRepository;
import ch.bbw.pr.tresorbackend.service.SafeDbCall;
import ch.bbw.pr.tresorbackend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * UserServiceImpl
 * @author Peter Rutschmann
 */
@Service
public class UserServiceImpl implements UserService {

   private final UserRepository userRepository;

   @Autowired
   public UserServiceImpl(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
   public User createUser(User user) {
      // Passwort kommt bereits gehasht (BCrypt + Pepper) vom UserController
      // Hier NICHT nochmals hashen!
      return SafeDbCall.safeDbCall(() -> userRepository.save(user), null);
   }

   @Override
   public User getUserById(Long userId) {
      Optional<User> user = SafeDbCall.safeDbCall(() -> userRepository.findById(userId), Optional.empty());
      if (user.isPresent()) return user.get();
      return null;
   }

   @Override
   public User findByEmail(String email) {
      Optional<User> user = SafeDbCall.safeDbCall(() -> userRepository.findByEmail(email), Optional.empty());
      if (user.isPresent()) return user.get();
      return null;
   }

   @Override
   public List<User> getAllUsers() {
      return (List<User>) SafeDbCall.safeDbCall(() -> userRepository.findAll(), List.of());
   }

   @Override
   public User updateUser(User user) {
      Optional<User> optionalExistingUser = SafeDbCall.safeDbCall(() -> userRepository.findById(user.getId()),
              Optional.empty());
      if (!optionalExistingUser.isPresent()) return null;
      User existingUser = optionalExistingUser.get();
      existingUser.setFirstName(user.getFirstName());
      existingUser.setLastName(user.getLastName());
      existingUser.setEmail(user.getEmail());
      return userRepository.save(existingUser);
   }

   @Override
   public boolean deleteUser(Long userId) {
      return SafeDbCall.safeDbCall(() -> userRepository.deleteById(userId));
   }
}