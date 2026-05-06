package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.ProgramItemView;
import com.graduation.fitmate.dto.ProgramSessionView;
import com.graduation.fitmate.dto.ProgramWeekView;
import com.graduation.fitmate.dto.TrainingProgramView;
import com.graduation.fitmate.entity.Exercise;
import com.graduation.fitmate.entity.TrainingProgram;
import com.graduation.fitmate.entity.TrainingProgramSession;
import com.graduation.fitmate.entity.TrainingProgramSessionItem;
import com.graduation.fitmate.entity.TrainingProgramWeek;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.entity.UserProgramEnrollment;
import com.graduation.fitmate.entity.WorkoutVideo;
import com.graduation.fitmate.mapper.ExerciseMapper;
import com.graduation.fitmate.mapper.TrainingProgramMapper;
import com.graduation.fitmate.mapper.TrainingProgramSessionItemMapper;
import com.graduation.fitmate.mapper.TrainingProgramSessionMapper;
import com.graduation.fitmate.mapper.TrainingProgramWeekMapper;
import com.graduation.fitmate.mapper.UserProgramEnrollmentMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingProgramService {

    private final UserAccountService userAccountService;
    private final UserProfileService userProfileService;
    private final WorkoutVideoService workoutVideoService;
    private final ExerciseMapper exerciseMapper;
    private final TrainingProgramMapper trainingProgramMapper;
    private final TrainingProgramWeekMapper trainingProgramWeekMapper;
    private final TrainingProgramSessionMapper trainingProgramSessionMapper;
    private final TrainingProgramSessionItemMapper trainingProgramSessionItemMapper;
    private final UserProgramEnrollmentMapper userProgramEnrollmentMapper;
    private final MessageSource messageSource;

    public TrainingProgramService(
            UserAccountService userAccountService,
            UserProfileService userProfileService,
            WorkoutVideoService workoutVideoService,
            ExerciseMapper exerciseMapper,
            TrainingProgramMapper trainingProgramMapper,
            TrainingProgramWeekMapper trainingProgramWeekMapper,
            TrainingProgramSessionMapper trainingProgramSessionMapper,
            TrainingProgramSessionItemMapper trainingProgramSessionItemMapper,
            UserProgramEnrollmentMapper userProgramEnrollmentMapper,
            MessageSource messageSource
    ) {
        this.userAccountService = userAccountService;
        this.userProfileService = userProfileService;
        this.workoutVideoService = workoutVideoService;
        this.exerciseMapper = exerciseMapper;
        this.trainingProgramMapper = trainingProgramMapper;
        this.trainingProgramWeekMapper = trainingProgramWeekMapper;
        this.trainingProgramSessionMapper = trainingProgramSessionMapper;
        this.trainingProgramSessionItemMapper = trainingProgramSessionItemMapper;
        this.userProgramEnrollmentMapper = userProgramEnrollmentMapper;
        this.messageSource = messageSource;
    }

    public TrainingProgramView latestProgram(String username) {
        UserAccount account = userAccountService.findByUsername(username);
        TrainingProgram program = trainingProgramMapper.selectOne(new LambdaQueryWrapper<TrainingProgram>()
                .eq(TrainingProgram::getUserId, account.getId())
                .orderByDesc(TrainingProgram::getCreatedAt)
                .last("limit 1"));
        if (program == null) {
            return null;
        }
        return buildProgramView(account.getId(), program);
    }

    @Transactional
    public TrainingProgramView generateFourWeekProgram(String username) {
        UserAccount account = userAccountService.findByUsername(username);
        UserProfile profile = userProfileService.getProfileByUsername(username);
        Locale locale = LocaleContextHolder.getLocale();
        String goal = profile != null && profile.getFitnessGoal() != null ? profile.getFitnessGoal() : "GENERAL";

        TrainingProgram program = new TrainingProgram();
        program.setUserId(account.getId());
        program.setTitle(messageSource.getMessage("program.generated.title", null, "4-week personalized training plan", locale));
        program.setSummary(messageSource.getMessage("program.generated.summary", null, "A progressive 4-week plan built from your profile, safety constraints, and the current exercise/video library.", locale));
        program.setGoal(goal);
        program.setDurationWeeks(4);
        program.setSessionsPerWeek(profile != null && profile.getWeeklyFrequency() != null ? Math.min(3, Math.max(2, profile.getWeeklyFrequency())) : 3);
        program.setStatus("ACTIVE");
        trainingProgramMapper.insert(program);

        UserProgramEnrollment enrollment = new UserProgramEnrollment();
        enrollment.setUserId(account.getId());
        enrollment.setProgramId(program.getId());
        enrollment.setCurrentWeek(1);
        enrollment.setCurrentSession(1);
        enrollment.setCompletedSessions(0);
        enrollment.setActive(true);
        userProgramEnrollmentMapper.insert(enrollment);

        List<WorkoutVideo> candidateVideos = selectCandidateVideos(profile);
        for (int weekNumber = 1; weekNumber <= 4; weekNumber++) {
            TrainingProgramWeek week = new TrainingProgramWeek();
            week.setProgramId(program.getId());
            week.setWeekNumber(weekNumber);
            week.setTitle(weekTitle(weekNumber, locale));
            week.setFocus(weekFocus(weekNumber, locale));
            week.setNotes(weekNotes(weekNumber, locale));
            trainingProgramWeekMapper.insert(week);

            for (int sessionNumber = 1; sessionNumber <= program.getSessionsPerWeek(); sessionNumber++) {
                TrainingProgramSession session = new TrainingProgramSession();
                session.setWeekId(week.getId());
                session.setSessionNumber(sessionNumber);
                session.setTitle(sessionTitle(weekNumber, sessionNumber, locale));
                session.setEstimatedMinutes(estimatedMinutes(profile, weekNumber));
                session.setIntensity(weekIntensity(weekNumber));
                trainingProgramSessionMapper.insert(session);

                List<WorkoutVideo> sessionVideos = pickSessionVideos(candidateVideos, weekNumber, sessionNumber);
                int sortOrder = 1;
                for (WorkoutVideo video : sessionVideos) {
                    TrainingProgramSessionItem item = new TrainingProgramSessionItem();
                    item.setSessionId(session.getId());
                    item.setVideoId(video.getId());
                    item.setExerciseId(findExerciseForVideo(video));
                    item.setSortOrder(sortOrder++);
                    item.setSetsCount(weekNumber <= 2 ? 1 : 2);
                    item.setRepsOrDuration(video.getDurationMinutes() + " min follow-along");
                    item.setRestSeconds(weekNumber <= 2 ? 60 : 45);
                    item.setInstruction(instructionFor(video, weekNumber, locale));
                    trainingProgramSessionItemMapper.insert(item);
                }
            }
        }
        return buildProgramView(account.getId(), program);
    }

    private TrainingProgramView buildProgramView(Long userId, TrainingProgram program) {
        TrainingProgramView view = new TrainingProgramView();
        view.setProgram(program);
        view.setEnrollment(userProgramEnrollmentMapper.selectOne(new LambdaQueryWrapper<UserProgramEnrollment>()
                .eq(UserProgramEnrollment::getUserId, userId)
                .eq(UserProgramEnrollment::getProgramId, program.getId())
                .last("limit 1")));
        List<TrainingProgramWeek> weeks = trainingProgramWeekMapper.selectList(new LambdaQueryWrapper<TrainingProgramWeek>()
                .eq(TrainingProgramWeek::getProgramId, program.getId())
                .orderByAsc(TrainingProgramWeek::getWeekNumber));
        for (TrainingProgramWeek week : weeks) {
            ProgramWeekView weekView = new ProgramWeekView();
            weekView.setWeek(week);
            List<TrainingProgramSession> sessions = trainingProgramSessionMapper.selectList(new LambdaQueryWrapper<TrainingProgramSession>()
                    .eq(TrainingProgramSession::getWeekId, week.getId())
                    .orderByAsc(TrainingProgramSession::getSessionNumber));
            for (TrainingProgramSession session : sessions) {
                ProgramSessionView sessionView = new ProgramSessionView();
                sessionView.setSession(session);
                List<TrainingProgramSessionItem> items = trainingProgramSessionItemMapper.selectList(new LambdaQueryWrapper<TrainingProgramSessionItem>()
                        .eq(TrainingProgramSessionItem::getSessionId, session.getId())
                        .orderByAsc(TrainingProgramSessionItem::getSortOrder));
                for (TrainingProgramSessionItem item : items) {
                    ProgramItemView itemView = new ProgramItemView();
                    itemView.setItem(item);
                    itemView.setVideo(item.getVideoId() == null ? null : workoutVideoService.findById(item.getVideoId()));
                    itemView.setExercise(item.getExerciseId() == null ? null : exerciseMapper.selectById(item.getExerciseId()));
                    sessionView.getItems().add(itemView);
                }
                weekView.getSessions().add(sessionView);
            }
            view.getWeeks().add(weekView);
        }
        return view;
    }

    private List<WorkoutVideo> selectCandidateVideos(UserProfile profile) {
        List<WorkoutVideo> activeVideos = new ArrayList<>(workoutVideoService.findAllActive());
        activeVideos.sort(Comparator.comparing(video -> scoreForProfile(video, profile), Comparator.reverseOrder()));
        return activeVideos.stream().limit(18).toList();
    }

    private int scoreForProfile(WorkoutVideo video, UserProfile profile) {
        int score = 0;
        if (profile == null) {
            return score;
        }
        if (equals(video.getTargetGoal(), profile.getFitnessGoal())) {
            score += 4;
        }
        if (contains(profile.getTargetAreas(), video.getTargetBodyPart()) || "FULL_BODY".equalsIgnoreCase(video.getTargetBodyPart())) {
            score += 3;
        }
        if (contains(profile.getAvailableEquipment(), video.getEquipmentRequirement()) || "NONE".equalsIgnoreCase(video.getEquipmentRequirement())) {
            score += 3;
        }
        if (profile.getKneeSensitive() != null && profile.getKneeSensitive() && "HIGH".equalsIgnoreCase(video.getImpactLevel())) {
            score -= 8;
        }
        if (profile.getBackSensitive() != null && profile.getBackSensitive() && "CORE".equalsIgnoreCase(video.getTargetBodyPart())) {
            score -= 3;
        }
        if ("LOW".equalsIgnoreCase(video.getImpactLevel())) {
            score += 1;
        }
        return score;
    }

    private List<WorkoutVideo> pickSessionVideos(List<WorkoutVideo> videos, int weekNumber, int sessionNumber) {
        if (videos.isEmpty()) {
            return List.of();
        }
        int offset = ((weekNumber - 1) * 3 + (sessionNumber - 1)) % videos.size();
        List<WorkoutVideo> picked = new ArrayList<>();
        for (int i = 0; i < Math.min(3, videos.size()); i++) {
            picked.add(videos.get((offset + i) % videos.size()));
        }
        return picked;
    }

    private Long findExerciseForVideo(WorkoutVideo video) {
        Exercise exercise = exerciseMapper.selectOne(new LambdaQueryWrapper<Exercise>()
                .eq(Exercise::getName, video.getTitle().length() > 160 ? video.getTitle().substring(0, 160) : video.getTitle())
                .last("limit 1"));
        return exercise == null ? null : exercise.getId();
    }

    private int estimatedMinutes(UserProfile profile, int weekNumber) {
        int base = profile != null && profile.getPreferredDurationMinutes() != null ? profile.getPreferredDurationMinutes() : 20;
        return Math.min(45, base + Math.max(0, weekNumber - 2) * 5);
    }

    private String weekIntensity(int weekNumber) {
        return switch (weekNumber) {
            case 1 -> "LOW";
            case 2 -> "LOW_MEDIUM";
            case 3 -> "MEDIUM";
            default -> "STEADY";
        };
    }

    private String weekTitle(int weekNumber, Locale locale) {
        return messageSource.getMessage("program.week." + weekNumber + ".title", null, "Week " + weekNumber, locale);
    }

    private String weekFocus(int weekNumber, Locale locale) {
        return messageSource.getMessage("program.week." + weekNumber + ".focus", null, "Progressive training focus", locale);
    }

    private String weekNotes(int weekNumber, Locale locale) {
        return messageSource.getMessage("program.week." + weekNumber + ".notes", null, "Keep movements controlled and stop if pain increases.", locale);
    }

    private String sessionTitle(int weekNumber, int sessionNumber, Locale locale) {
        return messageSource.getMessage("program.session.title", new Object[]{weekNumber, sessionNumber}, "Week " + weekNumber + " Session " + sessionNumber, locale);
    }

    private String instructionFor(WorkoutVideo video, int weekNumber, Locale locale) {
        return messageSource.getMessage(
                "program.item.instruction",
                new Object[]{video.getDurationMinutes(), weekNumber <= 2 ? 60 : 45},
                "Follow the video at comfortable intensity, then rest before the next item.",
                locale
        );
    }

    private boolean equals(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private boolean contains(String csv, String value) {
        if (csv == null || value == null) {
            return false;
        }
        return List.of(csv.split(",")).stream()
                .map(String::trim)
                .anyMatch(part -> part.equalsIgnoreCase(value));
    }
}
