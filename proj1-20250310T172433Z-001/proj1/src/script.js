// Initialize or load votes from localStorage
let votes = {
    "Candidate A": localStorage.getItem("Candidate A") ? parseInt(localStorage.getItem("Candidate A")) : 0,
    "Candidate B": localStorage.getItem("Candidate B") ? parseInt(localStorage.getItem("Candidate B")) : 0,
    "Candidate C": localStorage.getItem("Candidate C") ? parseInt(localStorage.getItem("Candidate C")) : 0
  };
  
  // Check if voter has already voted using localStorage
  function hasVoted(voterId) {
    return localStorage.getItem(`voter_${voterId}`) === "voted";
  }
  
  // Function to cast a vote
  function castVote() {
    const voterId = document.getElementById("voterId").value;
    const candidate = document.getElementById("candidate").value;
  
    if (voterId === "") {
      alert("Please enter your Voter ID.");
      return;
    }
  
    if (hasVoted(voterId)) {
      alert("You have already voted!");
      return;
    }
  
    // Record vote and mark voter as voted
    votes[candidate]++;
    localStorage.setItem(`voter_${voterId}`, "voted");
    localStorage.setItem(candidate, votes[candidate]);
  
    alert(`Vote casted for ${candidate} by Voter ID ${voterId}`);
    updateResults();
  }
  
  // Update the result display
  function updateResults() {
    document.getElementById("resultA").textContent = `Candidate A: ${votes["Candidate A"]} votes`;
    document.getElementById("resultB").textContent = `Candidate B: ${votes["Candidate B"]} votes`;
    document.getElementById("resultC").textContent = `Candidate C: ${votes["Candidate C"]} votes`;
  }
  
  // Initialize results on page load
  window.onload = updateResults;
  