plugins {
    id("testaiproject.jvm.library")
}

// No dependencies by design. These types are the vocabulary every other layer speaks,
// so anything added here is visible to the entire app.
