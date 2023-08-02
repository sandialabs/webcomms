package gov.sandia.webcomms.http.preemptors;

import gov.sandia.webcomms.http.HttpRequestWrapper;
import gov.sandia.webcomms.http.errors.PreemptionStoppedException;
import gov.sandia.webcomms.http.rsc.HttpResource;
import replete.plugins.StatelessProcess;

public abstract class HttpRequestPreemptor
        <P extends HttpRequestPreemptorParams> extends StatelessProcess<P> {


    //////////////////
    // CONSTRUCTORS //
    //////////////////

    public HttpRequestPreemptor(P params) {
        super(params);
    }


    //////////////
    // ABSTRACT //
    //////////////

    // Exceptions:
    //   MUST NOT throw an exception.
    //   Because "Http" does not do this and the
    //   preemptor should behave in the same manner.
    // Returning null:
    //   Returning null, means Http should handle the request as normal itself.
    //   Allows the preemptor to have its own baked-in criteria for what
    //   requests it can handle.  However, this is suboptimal design
    //   that should be fixed in the far future, because it implies that
    //   preemptors have control over the broader downloading process
    //   instead of just having the responsibility to populate an HttpResource
    //   object (separation of concerns violation).  Building the whether-or-
    //   not-to-handle logic within the method itself prevents higher-order
    //   user-specifiable criteria trees to be built and evaluated.
    // Provided:
    //   This method will be provided a HttpResource object
    //   with the following 2 or 3 fields populated:
    //     1. Original URL
    //     2. Cleaned URL (if options specified)
    //     3. HTTP Method (e.g. GET, POST)
    // Responsible For:
    //   The method is responsible for populating ANY other
    //   relevant fields.
    // TODO: Decide if preemptors should RESPECT or CONSIDER HttpRequestOptions,
    // and thus also change the UI to allow the user to specify the other
    // options AND a preemptor.
    public abstract HttpResource premptRequest(HttpRequestWrapper requestWrapper)
                                   throws PreemptionStoppedException;

    // Throwing a PreemptionStoppedException exception is a way to
    // indicate that the resource request itself is invalid.
    // Returning null is a declining to handle, asking Http to fetch.
    // Throwing any other exception is unexpected and should never happen
    // Http deals with that with its own wrapper exception.  But
    // PreemptionStoppedException fills that missing need to communicate
    // to the upper layers that this is just not a resource that should
    // be filled.  (returning a real resource is an indication that
    // it is a properly populated "fake" resource, whose own exceptions
    // should also be "fake").
}
