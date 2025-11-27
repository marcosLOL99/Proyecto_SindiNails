import React, { useEffect } from 'react';

const InstagramFeed = () => {
  useEffect(() => {
    const script = document.createElement('script');
    script.src = "https://static.elfsight.com/platform/platform.js";
    script.setAttribute('data-use-service-core', '');
    script.defer = true;
    document.body.appendChild(script);

    return () => {
      document.body.removeChild(script);
    };
  }, []);

  return <div className="elfsight-app-77cb1f37-5dd9-4aef-a85c-35db94df5a6e"  data-elfsight-app-lazy></div>;
};

export default InstagramFeed;
