const generateBtn = document.getElementById("generateBtn");

const loading = document.getElementById("loading");

const result = document.getElementById("result");

const summaryTab = document.getElementById("summaryTab");

const originalTab = document.getElementById("originalTab");

const translatedTab = document.getElementById("translatedTab");

const tabs = document.querySelectorAll(".tab");

const copyBtn = document.getElementById("copyBtn");

const exportTxtBtn = document.getElementById("exportTxtBtn");

const exportPdfBtn = document.getElementById("exportPdfBtn");


// ACTIVE TAB TRACKING
let activeTabContent = "";


// TAB SWITCHING
tabs.forEach(tab => {

    tab.addEventListener("click", () => {

        tabs.forEach(t => t.classList.remove("active"));

        document.querySelectorAll(".tab-content")
            .forEach(content => content.classList.remove("active"));

        tab.classList.add("active");

        const target = tab.getAttribute("data-tab");

        const targetElement = document.getElementById(target);

        targetElement.classList.add("active");

        activeTabContent = targetElement.textContent;
    });
});


// COPY BUTTON
copyBtn.addEventListener("click", async () => {

    try {

        await navigator.clipboard.writeText(activeTabContent);

        copyBtn.textContent = "Copied!";

        setTimeout(() => {
            copyBtn.textContent = "Copy";
        }, 1500);

    } catch (error) {

        console.error(error);

        alert("Failed to copy.");
    }
});

// EXPORT TXT
exportTxtBtn.addEventListener("click", () => {

    try {

        const blob = new Blob(
            [activeTabContent],
            { type: "text/plain" }
        );

        const url = URL.createObjectURL(blob);

        const a = document.createElement("a");

        a.href = url;

        a.download = "yt-ai-content.txt";

        document.body.appendChild(a);

        a.click();

        document.body.removeChild(a);

        URL.revokeObjectURL(url);

    } catch (error) {

        console.error(error);

        alert("TXT export failed.");
    }
});

// EXPORT BEAUTIFUL PDF
exportPdfBtn.addEventListener("click", async () => {

    try {

        // TEMPLATE ELEMENTS
        const pdfTemplate =
            document.getElementById("pdfTemplate");

        const pdfContent =
            document.getElementById("pdfContent");

        const pdfMode =
            document.getElementById("pdfMode");

        const pdfLanguage =
            document.getElementById("pdfLanguage");

        const pdfDate =
            document.getElementById("pdfDate");

        // USER VALUES
        const mode =
            document.getElementById("mode").value;

        const language =
            document.getElementById("language").value || "Original";

        // FILL TEMPLATE
        pdfMode.textContent = mode;

        pdfLanguage.textContent = language;

        pdfDate.textContent =
            new Date().toLocaleString();

        pdfContent.textContent =
            activeTabContent;

        // HTML -> CANVAS
        const canvas = await html2canvas(pdfTemplate, {
            scale: 2
        });

        const imgData =
            canvas.toDataURL("image/png");

        const { jsPDF } = window.jspdf;

        const pdf = new jsPDF("p", "mm", "a4");

        const pdfWidth =
            pdf.internal.pageSize.getWidth();

        const pdfHeight =
            (canvas.height * pdfWidth) / canvas.width;

        pdf.addImage(
            imgData,
            "PNG",
            0,
            0,
            pdfWidth,
            pdfHeight
        );

        pdf.save("yt-ai-notes.pdf");

    } catch (error) {

        console.error(error);

        alert("Beautiful PDF export failed.");
    }
});


// GENERATE BUTTON
generateBtn.addEventListener("click", async () => {

    loading.classList.remove("hidden");

    result.classList.add("hidden");

    generateBtn.disabled = true;

    generateBtn.textContent = "Generating...";

    try {

        // CURRENT TAB
        const [tab] = await chrome.tabs.query({
            active: true,
            currentWindow: true
        });

        const videoUrl = tab.url;

        // USER OPTIONS
        const mode =
            document.getElementById("mode").value;

        const language =
            document.getElementById("language").value;

        // API CALL
        const response = await fetch(
            "http://localhost:8080/api/video/summarize",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    url: videoUrl,
                    outputLanguage: language,
                    mode: mode
                })
            }
        );

        const data = await response.json();

        console.log(data);

        // HANDLE ERRORS
        if (!response.ok) {

            alert(data.error || "Something went wrong.");

            loading.classList.add("hidden");

            generateBtn.disabled = false;

            generateBtn.textContent = "Generate";

            return;
        }

        // UPDATE CONTENT
        summaryTab.textContent =
            data.summary || "No summary available.";

        originalTab.textContent =
            data.originalTranscript || "No transcript available.";

        translatedTab.textContent =
            data.translatedTranscript || "No translation available.";

        // DEFAULT ACTIVE CONTENT
        activeTabContent = summaryTab.textContent;

        result.classList.remove("hidden");

    } catch (error) {

        console.error(error);

        alert("Backend connection failed.");
    }

    loading.classList.add("hidden");

    generateBtn.disabled = false;

    generateBtn.textContent = "Generate";
});