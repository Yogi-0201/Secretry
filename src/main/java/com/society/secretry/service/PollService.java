package com.society.secretry.service;

import com.society.secretry.model.Poll;
import com.society.secretry.model.PollVote;
import com.society.secretry.repository.PollRepository;
import com.society.secretry.repository.PollVoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollVoteRepository pollVoteRepository;

    public static class PollDetails {
        private Poll poll;
        private boolean hasVoted;
        private String userVote;
        private Map<String, Long> results;
        private long totalVotes;

        public PollDetails(Poll poll, boolean hasVoted, String userVote, Map<String, Long> results, long totalVotes) {
            this.poll = poll;
            this.hasVoted = hasVoted;
            this.userVote = userVote;
            this.results = results;
            this.totalVotes = totalVotes;
        }

        public Poll getPoll() { return poll; }
        public boolean isHasVoted() { return hasVoted; }
        public String getUserVote() { return userVote; }
        public Map<String, Long> getResults() { return results; }
        public long getTotalVotes() { return totalVotes; }
    }

    public Poll createPoll(String question, String description, String options, Integer durationDays) {
        LocalDateTime expiresAt = durationDays != null ? LocalDateTime.now().plusDays(durationDays) : null;
        Poll poll = new Poll(question, description, options, expiresAt);
        return pollRepository.save(poll);
    }

    public List<Poll> getAllPolls() {
        List<Poll> polls = pollRepository.findAllByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();
        for (Poll poll : polls) {
            if ("ACTIVE".equals(poll.getStatus()) && poll.getExpiresAt() != null && now.isAfter(poll.getExpiresAt())) {
                poll.setStatus("CLOSED");
                pollRepository.save(poll);
            }
        }
        return polls;
    }

    public Optional<Poll> getPollById(Long id) {
        return pollRepository.findById(id);
    }

    public PollVote castVote(Long pollId, String flatNumber, String selectedOption) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found with id: " + pollId));

        if ("CLOSED".equals(poll.getStatus())) {
            throw new RuntimeException("This voting session has been closed.");
        }

        if (poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            poll.setStatus("CLOSED");
            pollRepository.save(poll);
            throw new RuntimeException("This voting session has already expired.");
        }

        List<String> optionsList = Arrays.asList(poll.getOptions().split(","));
        boolean validOption = false;
        for (String opt : optionsList) {
            if (opt.trim().equalsIgnoreCase(selectedOption.trim())) {
                validOption = true;
                selectedOption = opt.trim(); // use exact case from options
                break;
            }
        }

        if (!validOption) {
            throw new RuntimeException("Invalid vote option selected: " + selectedOption);
        }

        Optional<PollVote> existingVote = pollVoteRepository.findByPollIdAndFlatNumber(pollId, flatNumber);
        if (existingVote.isPresent()) {
            throw new RuntimeException("Flat " + flatNumber + " has already cast a vote for this decision.");
        }

        PollVote vote = new PollVote(pollId, flatNumber, selectedOption);
        return pollVoteRepository.save(vote);
    }

    public Poll closePoll(Long pollId) {
        return pollRepository.findById(pollId).map(poll -> {
            poll.setStatus("CLOSED");
            return pollRepository.save(poll);
        }).orElseThrow(() -> new RuntimeException("Poll not found with id: " + pollId));
    }

    public PollDetails getPollDetails(Long pollId, String userFlatNumber) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new RuntimeException("Poll not found with id: " + pollId));

        if ("ACTIVE".equals(poll.getStatus()) && poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            poll.setStatus("CLOSED");
            pollRepository.save(poll);
        }

        Optional<PollVote> userVoteOpt = userFlatNumber != null 
                ? pollVoteRepository.findByPollIdAndFlatNumber(pollId, userFlatNumber)
                : Optional.empty();

        boolean hasVoted = userVoteOpt.isPresent();
        String userVote = hasVoted ? userVoteOpt.get().getSelectedOption() : null;

        List<PollVote> votes = pollVoteRepository.findByPollId(pollId);
        long totalVotes = votes.size();

        Map<String, Long> resultsMap = new LinkedHashMap<>();
        String[] opts = poll.getOptions().split(",");
        for (String opt : opts) {
            resultsMap.put(opt.trim(), 0L);
        }

        for (PollVote vote : votes) {
            String sel = vote.getSelectedOption().trim();
            resultsMap.put(sel, resultsMap.getOrDefault(sel, 0L) + 1);
        }

        return new PollDetails(poll, hasVoted, userVote, resultsMap, totalVotes);
    }
}
