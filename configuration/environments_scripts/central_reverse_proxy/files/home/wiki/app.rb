require 'gollum/app'
require 'digest/sha1'
require 'logger'
require 'rest-client'


class App < Precious::App
  use Rack::Session::Pool, :cookie_only => false
  User = Struct.new(:name, :email, :password_hash, :can_write)
  LOGGER = Logger.new("/home/wiki/wiki_log.txt") 
  CLIENT_ID = ""
  CLIENT_SECRET = ""
  before { check! }
  before '/gollum/(edit|create/rename)/*' do authorize_write end
  before do
    if session[:email] && session[:name] 
        session['gollum.author'] = {
        :name => session[:name],
        :email => session[:email]
        }
    end
  end

  get '/logout' do
    session.clear()
    'logged out'
  end

  get '/login' do
    session[:oauth_state] = SecureRandom.hex(16)
    params = {
        client_id: CLIENT_ID,
        scope: 'public_repo user:email',
        state: session[:oauth_state],
        redirect_uri: "https://git.sapsailing.com/cgi-bin/github_oauth.sh"
    }
    uri = URI::HTTPS.build(
    host: 'github.com',
    path: '/login/oauth/authorize',
    query: URI.encode_www_form(params)
    )
    redirect uri.to_s()
  end

  get '/callback' do
    halt 401, params[:error] if params[:error]
    halt 400, "Missing code" unless params[:code]
    halt 403, "Invalid OAuth state" unless session[:oauth_state] ==  params[:state]

    session.delete(:oauth_state)

    result = JSON.parse(RestClient.post('https://github.com/login/oauth/access_token',
        {
            :client_id => CLIENT_ID,
            :client_secret => CLIENT_SECRET,
            :code => params[:code],
            :redirect_uri => "https://git.sapsailing.com/cgi-bin/github_oauth.sh"
        },
        :accept => :json))
    LOGGER.debug(result)
    scopes = result['scope'].split(',')
    if scopes.include?('user:email') && scopes.include?('public_repo') && result['access_token']
        session[:access_token] = result['access_token']
        LOGGER.debug(session[:access_token])
        emails =  JSON.parse(RestClient.get('https://api.github.com/user/emails',
            {
            :Authorization => "Bearer #{session[:access_token]}"
            }))
        if emails[0] && emails[0]['email']
            session[:email] = emails[0]['email']
            LOGGER.debug("email is #{session[:email]}")
        end

        user_details = JSON.parse(RestClient.get('https://api.github.com/user',
            {
            :Authorization => "Bearer #{session[:access_token]}"
            }))
        if user_details['login'] && user_details['id']
            session[:user_id] = user_details['id']
            session[:name] = user_details['login']
            LOGGER.debug("user #{session[:email]} logged in")
        end
    end
    
    if session[:prev] 
        redirect session[:prev]
    end

    redirect "/"
  end



  helpers do
    def public_path?(path)
        if path == "/" 
          return true
        end
        public_starts = [ "/Home", "/wiki", "/favicon.ico"] 
        return true if public_starts.any? {|link| path.start_with?(link)}
        public_patterns = [ %r{\A/gollum/(assets|commit|history|last_commit_info).*}, %r{\A/gollum/search}, %r{\A/gollum/latest_changes\z}]
        public_patterns.any? {|pattern| pattern.match(path)}
    end

    def auth_path?(path)
      auth_paths = [%r{\A/gollum/(edit|create|rename)/.*\z}, %r{\A/gollum/overview}]  
      auth_paths.any? {|pattern| pattern.match(path)}
    end

    def login_path?(path)
        %r{\A/(login|callback|logout)\z}.match?(path)
    end

    def check!
      path = self.env['PATH_INFO']
      return if login_path?(path)
      session[:prev] = path
      return if public_path?(path) 
      halt 403, 'Forbidden - You can not access anything outside wiki/ path.' unless auth_path?(path)      
    end

    def authorize_write
      LOGGER.debug("Checking auth before writing")
      if !session[:access_token]
        redirect '/login'
      end
      halt 403, "Forbidden" unless user_can_write()
    end

    def user_can_write() 
        return false unless session[:access_token]
        result = JSON.parse(RestClient.get('https://api.github.com/repos/SAP/sailing-analytics',
            {
            :Authorization => "Bearer #{session[:access_token]}"
            }))
        result.dig('permissions', 'push') == true
    rescue RestClient::ExceptionWithResponse
        false
    end
  end
end
