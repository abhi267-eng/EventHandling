import { Link } from "react-router-dom";

function Landing() {
  const eventCategories = [
    {
      title: "Cultural Events",
      image: "/event-images/CulturalEvent.jpeg",
      description:
        "Celebrate creativity, talent, traditions and culture through exciting college events.",
    },
    {
      title: "Sports Events",
      image: "/event-images/SportEvent.jpg",
      description:
        "Discover tournaments, competitions and sports activities happening across colleges.",
    },
    {
      title: "Technical Events",
      image: "/event-images/TechnicalEvent.jpg",
      description:
        "Explore hackathons, coding competitions, technical challenges and technology events.",
    },
    {
      title: "Social Events",
      image: "/event-images/SocialEvent.jpeg",
      description:
        "Take part in community activities, awareness programs and social initiatives.",
    },
    {
      title: "Workshops",
      image: "/event-images/Workshop.jpg",
      description:
        "Learn practical skills through workshops conducted by students and professionals.",
    },
    {
      title: "Seminars",
      image: "/event-images/Seminar.jpg",
      description:
        "Attend informative talks, expert sessions and knowledge-sharing events.",
    },
    {
      title: "Fests",
      image: "/event-images/Fest.jpeg",
      description:
        "Experience the biggest college celebrations, festivals and campus gatherings.",
    },
  ];

  return (
    <div className="landing-page">
      <div className="stars"></div>

      {/* Navigation */}
      <nav className="landing-navbar">
        <Link className="landing-logo" to="/">
          Event<span>Book</span>
        </Link>

        <div className="landing-nav-links">
          <a href="#about">About</a>
          <a href="#events">Events</a>
          <a href="#how-it-works">How It Works</a>
          <a href="#contact">Contact</a>

          <Link className="nav-login" to="/login">
            Login
          </Link>

          <Link className="nav-signup" to="/signup">
            Signup
          </Link>
        </div>
      </nav>

      <main>
        {/* Hero */}
        <section className="landing-hero">
          <div className="hero-content">
            <p className="hero-tag">
              YOUR CAMPUS. YOUR EVENTS. YOUR EXPERIENCE.
            </p>

            <h1>
              Discover Events.
              <br />
              <span>Make Memories.</span>
            </h1>

            <p className="hero-description">
              EventBook brings college events from different campuses together
              in one place. Discover, explore and never miss an event worth
              attending.
            </p>

            <div className="hero-buttons">
              <Link className="hero-primary-button" to="/signup">
                Get Started
              </Link>

              <Link className="hero-secondary-button" to="/login">
                Login
              </Link>
            </div>
          </div>

          <div className="hero-visual">
            <div className="hero-glow"></div>

            <div className="hero-event-card hero-card-one">
              <span>TECH</span>
              <strong>Build. Learn. Innovate.</strong>
            </div>

            <div className="hero-event-card hero-card-two">
              <span>CULTURE</span>
              <strong>Celebrate Your Talent.</strong>
            </div>

            <div className="hero-event-card hero-card-three">
              <span>SPORTS</span>
              <strong>Compete. Connect. Win.</strong>
            </div>
          </div>
        </section>

        {/* About */}
        <section className="landing-section" id="about">
          <div className="section-heading">
            <p>ABOUT EVENTBOOK</p>

            <h2>One place for every college event</h2>

            <span>
              EventBook is a college event discovery platform designed to
              connect students, organizers, faculty and colleges through
              events.
            </span>
          </div>

          <div className="about-grid">
            <div className="about-card">
              <h3>Discover</h3>
              <p>
                Find events happening in your college and discover events from
                other colleges that welcome visitors.
              </p>
            </div>

            <div className="about-card">
              <h3>Explore</h3>
              <p>
                Browse different categories such as technical events, sports,
                cultural programs, workshops, seminars and fests.
              </p>
            </div>

            <div className="about-card">
              <h3>Connect</h3>
              <p>
                Bring students and event organizers together through a
                centralized platform built specifically for college events.
              </p>
            </div>
          </div>
        </section>

        {/* Events */}
        <section className="landing-section" id="events">
          <div className="section-heading">
            <p>EXPLORE EVENTS</p>

            <h2>Something for everyone</h2>

            <span>
              Whether you love technology, sports, culture or learning,
              discover events that match your interests.
            </span>
          </div>

          <div className="event-category-grid">
            {eventCategories.map((event) => (
              <article className="event-category-card" key={event.title}>
                <div className="event-image-wrapper">
                  <img src={event.image} alt={event.title} />
                </div>

                <div className="event-category-content">
                  <h3>{event.title}</h3>

                  <p>{event.description}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* How It Works */}
        <section className="how-section" id="how-it-works">
          <div className="section-heading">
            <p>HOW IT WORKS</p>

            <h2>From discovery to experience</h2>

            <span>
              EventBook keeps finding and exploring college events simple.
            </span>
          </div>

          <div className="steps-grid">
            <div className="step-card">
              <div className="step-number">01</div>

              <h3>Create an account</h3>

              <p>
                Sign up on EventBook and create your profile according to your
                role.
              </p>
            </div>

            <div className="step-card">
              <div className="step-number">02</div>

              <h3>Discover events</h3>

              <p>
                Browse events from your college and other colleges that allow
                visitors.
              </p>
            </div>

            <div className="step-card">
              <div className="step-number">03</div>

              <h3>Explore the details</h3>

              <p>
                Check the event description, date, time, venue, rules and
                organizer information.
              </p>
            </div>

            <div className="step-card">
              <div className="step-number">04</div>

              <h3>Save what interests you</h3>

              <p>
                Save interesting events so you can easily find them again
                later.
              </p>
            </div>
          </div>
        </section>

        {/* Contact */}
        <section className="contact-section" id="contact">
          <div className="contact-card">
            <div>
              <p>CONTACT</p>

              <h2>Have a question?</h2>

              <span>
                Get in touch with the EventBook team.
              </span>
            </div>

            <div className="contact-emails">
              <a href="mailto:vu2f2425053@pvppcoe.ac.in">
                vu2f2425053@pvppcoe.ac.in
              </a>

              <a href="mailto:vu2f2425043@pvppcoe.ac.in">
                vu2f2425043@pvppcoe.ac.in
              </a>

              <a href="mailto:vu2f2425047@pvppcoe.ac.in">
                vu2f2425047@pvppcoe.ac.in
              </a>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="landing-footer">
        <strong>
          Event<span>Book</span>
        </strong>

        <p>College events, all in one place.</p>
      </footer>
    </div>
  );
}

export default Landing;